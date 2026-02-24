package com.ktb.interview.application.service;

import com.ktb.ai.feedback.exception.AiFeedbackDependencyFailedException;
import com.ktb.ai.feedback.exception.AiFeedbackRequestRejectedException;
import com.ktb.ai.feedback.exception.AiFeedbackRetryableException;
import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.domain.TurnType;
import com.ktb.interview.session.mapper.InterviewSessionFeedbackMapper;
import com.ktb.interview.dto.ai.InterviewFollowUpQuestionApiResponse;
import com.ktb.interview.dto.ai.InterviewFollowUpQuestionDataResponse;
import com.ktb.interview.dto.ai.InterviewFollowUpQuestionRequest;
import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.session.dto.request.InterviewSessionCreateRequest;
import com.ktb.interview.session.dto.response.InterviewHistoryResponse;
import com.ktb.interview.session.dto.response.InterviewSessionCreateResponse;
import com.ktb.interview.session.dto.response.InterviewSessionStateResponse;
import com.ktb.interview.session.dto.response.SessionFeedbackFailedResponse;
import com.ktb.interview.session.dto.response.SessionFeedbackPendingResponse;
import com.ktb.interview.application.InterviewSessionManagementService;
import com.ktb.interview.port.out.AiInterviewPort;
import com.ktb.interview.session.domain.InterviewHistoryItem;
import com.ktb.interview.session.domain.InterviewSessionFeedback;
import com.ktb.interview.session.domain.InterviewQuestionSnapshot;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.domain.InterviewSessionStatus;
import com.ktb.interview.session.exception.InterviewSessionInvalidInputException;
import com.ktb.interview.session.exception.InterviewSessionInvalidStateException;
import com.ktb.interview.session.repository.InterviewSessionFeedbackRepository;
import com.ktb.interview.session.service.InterviewSessionService;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 인터뷰 세션 관리 유스케이스 구현체.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewSessionManagementServiceImpl implements InterviewSessionManagementService {

    private static final String ERROR_PENDING_STATE_REQUIRED = "session is not in pending state";
    private static final String ERROR_FAILED_STATE_REQUIRED = "session is not failed";
    private static final String ERROR_FEEDBACK_NOT_COMPLETED = "session feedback is not completed yet";
    private static final String ERROR_UNSUPPORTED_INTERVIEW_TYPE =
            "interviewType supports only PRACTICE_INTERVIEW or REAL_INTERVIEW";
    private static final String ERROR_CATEGORY_NOT_SUPPORTED_TEMPLATE =
            "category is not supported for questionType. category=%s, questionType=%s";
    private static final String ERROR_FIRST_QUESTION_GENERATION_FAILED = "failed to generate first question from AI";

    private final InterviewSessionService interviewSessionService;
    private final InterviewSessionFeedbackRepository feedbackRepository;
    private final AiInterviewPort aiInterviewPort;

    /**
     * 연습/실전 모드별 초기 상태를 세팅해 인터뷰 세션을 생성합니다.
     */
    @Override
    public InterviewSessionCreateResponse createSession(Long accountId, InterviewSessionCreateRequest request) {
        log.info("createSession - accountId={}, interviewType={}, questionType={}, category={}",
                accountId, request.interviewType(), request.questionType(), request.category());
        validateSessionCreateRequest(request);

        if (request.interviewType() == AnswerType.PRACTICE_INTERVIEW) {
            InterviewSession session = interviewSessionService.createPracticeSession(
                    accountId,
                    request.questionType()
            );
            log.info("Practice session created - accountId={}, sessionId={}, expiresAt={}",
                    accountId, session.getSessionId(), session.getExpiresAt());

            return new InterviewSessionCreateResponse(
                    session.getSessionId(),
                    session.getInterviewType().name(),
                    session.getQuestionType().name(),
                    null,
                    null,
                    TurnType.NEW_TOPIC.wireValue(),
                    null,
                    session.getExpiresAt().toString()
            );
        }

        InterviewSession session = interviewSessionService.createRealSession(
                accountId,
                request.questionType()
        );
        log.info("Real session skeleton created - accountId={}, sessionId={}, expiresAt={}",
                accountId, session.getSessionId(), session.getExpiresAt());

        try {
            FirstQuestionDecision firstQuestionDecision = requestFirstQuestionFromAi(session, request.category());
            QuestionCategory responseCategory = resolveFirstQuestionCategory(
                    request.category(),
                    firstQuestionDecision.category()
            );
            InterviewQuestionSnapshot firstQuestion = new InterviewQuestionSnapshot(
                    null,
                    firstQuestionDecision.questionText(),
                    responseCategory
            );

            TurnType firstTurnType = resolveFirstTurnType(firstQuestionDecision.turnType());
            Integer firstTopicId = firstQuestionDecision.topicId() == null ? 1 : firstQuestionDecision.topicId();

            session.updateNextQuestion(firstQuestion, firstTurnType, firstTopicId);
            interviewSessionService.save(session);
            log.info("Real session first question prepared - sessionId={}, topicId={}, turnType={}, category={}",
                    session.getSessionId(), firstTopicId, firstTurnType, responseCategory);

            return new InterviewSessionCreateResponse(
                    session.getSessionId(),
                    session.getInterviewType().name(),
                    session.getQuestionType().name(),
                    session.getCurrentQuestion() == null ? null : session.getCurrentQuestion().content(),
                    session.getCurrentQuestion() == null || session.getCurrentQuestion().category() == null
                            ? null
                            : session.getCurrentQuestion().category().name(),
                    firstTurnType.wireValue(),
                    firstTopicId,
                    session.getExpiresAt().toString()
            );
        } catch (AiFeedbackRequestRejectedException e) {
            log.warn("Real session creation rejected by AI - accountId={}, sessionId={}, reason={}",
                    accountId, session.getSessionId(), e.getMessage());
            interviewSessionService.deleteSession(session.getSessionId());
            throw e;
        } catch (AiFeedbackRetryableException e) {
            log.warn("Real session creation failed by retryable AI error - accountId={}, sessionId={}, reason={}",
                    accountId, session.getSessionId(), e.getMessage());
            interviewSessionService.deleteSession(session.getSessionId());
            throw new AiFeedbackDependencyFailedException(e.getMessage(), e);
        } catch (RuntimeException e) {
            log.warn("Real session creation failed unexpectedly - accountId={}, sessionId={}, reason={}",
                    accountId, session.getSessionId(), e.getMessage());
            interviewSessionService.deleteSession(session.getSessionId());
            throw e;
        }
    }

    /**
     * 세션 현재 상태와 누적 이력을 조회해 응답 모델로 변환합니다.
     * 조회 시 TTL은 연장하지 않습니다.
     */
    @Override
    public InterviewSessionStateResponse getSessionState(Long accountId, String sessionId) {
        InterviewSession session = interviewSessionService.getSessionWithoutTouch(accountId, sessionId);
        List<InterviewHistoryItem> history = session.getInterviewHistoryView();
        InterviewHistoryItem last = history.isEmpty() ? null : history.get(history.size() - 1);
        log.debug("getSessionState - accountId={}, sessionId={}, status={}, turnCount={}, historySize={}",
                accountId, sessionId, session.getStatus(), session.getTurnCount(), history.size());

        return new InterviewSessionStateResponse(
                session.getSessionId(),
                session.getInterviewType().name(),
                session.getStatus().name(),
                session.getTurnCount(),
                session.getCurrentTopicId(),
                last == null ? null : last.turnType().wireValue(),
                last == null ? null : last.question(),
                session.getStatus() == InterviewSessionStatus.COMPLETED,
                session.getExpiresAt() == null ? null : session.getExpiresAt().toString(),
                history.stream()
                        .map(item -> new InterviewHistoryResponse(
                                item.question(),
                                item.category() == null ? null : item.category().name(),
                                item.answerText(),
                                item.turnType().wireValue(),
                                item.turnOrder(),
                                item.topicId()
                        ))
                        .toList()
        );
    }

    /**
     * 피드백 생성 진행 중 세션의 대기 상태 정보를 반환합니다.
     */
    @Override
    public SessionFeedbackPendingResponse getSessionFeedbackPending(Long accountId, String sessionId) {
        InterviewSession session = interviewSessionService.getSessionWithoutTouch(accountId, sessionId);
        if (session.getStatus() != InterviewSessionStatus.IN_PROGRESS
                && session.getStatus() != InterviewSessionStatus.RETRYING) {
            throw new InterviewSessionInvalidStateException(ERROR_PENDING_STATE_REQUIRED);
        }
        log.debug("getSessionFeedbackPending - accountId={}, sessionId={}, status={}, retryCount={}",
                accountId, sessionId, session.getStatus(), session.getRetryCount());

        return new SessionFeedbackPendingResponse(
                session.getStatus().name(),
                session.getRetryCount(),
                session.getNextRetryAt() == null ? null : session.getNextRetryAt().toString()
        );
    }

    /**
     * 피드백 생성이 실패한 세션의 실패 정보를 반환합니다.
     */
    @Override
    public SessionFeedbackFailedResponse getSessionFeedbackFailed(Long accountId, String sessionId) {
        InterviewSession session = interviewSessionService.getSessionWithoutTouch(accountId, sessionId);
        if (session.getStatus() != InterviewSessionStatus.FAILED) {
            throw new InterviewSessionInvalidStateException(ERROR_FAILED_STATE_REQUIRED);
        }
        log.debug("getSessionFeedbackFailed - accountId={}, sessionId={}, errorCode={}, retryCount={}",
                accountId, sessionId, session.getErrorCode(), session.getRetryCount());

        return new SessionFeedbackFailedResponse(
                session.getStatus().name(),
                session.getErrorMessage(),
                session.getRetryCount(),
                session.getFailedAt() == null ? null : session.getFailedAt().toString(),
                session.getSessionId(),
                session.getErrorCode()
        );
    }

    /**
     * 완료된 세션의 최종 피드백을 반환합니다.
     */
    @Override
    public InterviewFeedbackDataResponse getSessionFeedbackCompleted(Long accountId, String sessionId) {
        InterviewSession session = interviewSessionService.getSessionWithoutTouch(accountId, sessionId);
        Optional<InterviewSessionFeedback> feedback = feedbackRepository.findBySessionId(sessionId);
        if (session.getStatus() != InterviewSessionStatus.COMPLETED || feedback.isEmpty()) {
            throw new InterviewSessionInvalidStateException(ERROR_FEEDBACK_NOT_COMPLETED);
        }
        log.info("getSessionFeedbackCompleted - accountId={}, sessionId={}, status={}",
                accountId, sessionId, session.getStatus());
        return InterviewSessionFeedbackMapper.toDto(feedback.get());
    }

    /**
     * 세션 생성 요청의 인터뷰 유형 지원 범위를 검증합니다.
     */
    private void validateSessionCreateRequest(InterviewSessionCreateRequest request) {
        if (request.interviewType() != AnswerType.PRACTICE_INTERVIEW
                && request.interviewType() != AnswerType.REAL_INTERVIEW) {
            throw new InterviewSessionInvalidInputException(ERROR_UNSUPPORTED_INTERVIEW_TYPE);
        }

        if (request.category() != null && !request.category().supports(request.questionType())) {
            throw new InterviewSessionInvalidInputException(
                    String.format(ERROR_CATEGORY_NOT_SUPPORTED_TEMPLATE, request.category(), request.questionType())
            );
        }
    }

    /**
     * 실전 세션 생성 시 AI 서버에 첫 질문 생성을 요청합니다.
     */
    private FirstQuestionDecision requestFirstQuestionFromAi(InterviewSession session, QuestionCategory requestedCategory) {
        QuestionCategory initialCategory = resolveInitialCategoryForFirstQuestion(session.getQuestionType(), requestedCategory);
        log.debug("requestFirstQuestionFromAi - sessionId={}, questionType={}, requestedCategory={}, initialCategory={}",
                session.getSessionId(), session.getQuestionType(), requestedCategory, initialCategory);
        InterviewFollowUpQuestionRequest aiRequest = new InterviewFollowUpQuestionRequest(
                session.getAccountId(),
                session.getSessionId(),
                session.getQuestionType().name(),
                initialCategory,
                List.of()
        );

        InterviewFollowUpQuestionApiResponse aiResponse = aiInterviewPort.requestFollowUpQuestion(aiRequest);
        InterviewFollowUpQuestionDataResponse data = aiResponse.data();

        boolean isSessionEnded = "session_ended".equalsIgnoreCase(aiResponse.message())
                || Boolean.TRUE.equals(data.isSessionEnded());
        if (isSessionEnded || data.questionText() == null || data.questionText().isBlank()) {
            throw new InterviewSessionInvalidStateException(ERROR_FIRST_QUESTION_GENERATION_FAILED);
        }
        log.debug("First question from AI received - sessionId={}, topicId={}, turnType={}, category={}",
                session.getSessionId(), data.topicId(), data.turnType(), data.category());

        return new FirstQuestionDecision(
                data.questionText(),
                data.category(),
                data.turnType(),
                data.topicId()
        );
    }

    private QuestionCategory resolveInitialCategoryForFirstQuestion(QuestionType questionType, QuestionCategory requestedCategory) {
        if (requestedCategory != null) {
            return requestedCategory;
        }

        List<QuestionCategory> candidates = new ArrayList<>();
        for (QuestionCategory category : QuestionCategory.values()) {
            if (category.supports(questionType)) {
                candidates.add(category);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }

        int index = ThreadLocalRandom.current().nextInt(candidates.size());
        return candidates.get(index);
    }

    /**
     * AI 첫 질문 turn_type을 내부 enum으로 파싱합니다.
     */
    private TurnType resolveFirstTurnType(String turnType) {
        if (turnType == null || turnType.isBlank()) {
            return TurnType.MAIN;
        }
        try {
            return TurnType.fromWireValue(turnType);
        } catch (IllegalArgumentException ignored) {
            return TurnType.MAIN;
        }
    }

    /**
     * 첫 질문 카테고리 우선순위: 사용자 요청 > AI 응답 category.
     */
    private QuestionCategory resolveFirstQuestionCategory(
            QuestionCategory requestCategory,
            String aiResponseCategory
    ) {
        if (requestCategory != null) {
            return requestCategory;
        }
        return parseQuestionCategory(aiResponseCategory);
    }

    private QuestionCategory parseQuestionCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        try {
            return QuestionCategory.valueOf(category.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private record FirstQuestionDecision(
            String questionText,
            String category,
            String turnType,
            Integer topicId
    ) {
    }
}
