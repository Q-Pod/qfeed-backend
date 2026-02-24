package com.ktb.interview.application.service;

import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.domain.TurnType;
import com.ktb.interview.session.mapper.InterviewSessionFeedbackMapper;
import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.session.dto.request.InterviewSessionCreateRequest;
import com.ktb.interview.session.dto.response.InterviewHistoryResponse;
import com.ktb.interview.session.dto.response.InterviewSessionCreateResponse;
import com.ktb.interview.session.dto.response.InterviewSessionStateResponse;
import com.ktb.interview.session.dto.response.SessionFeedbackFailedResponse;
import com.ktb.interview.session.dto.response.SessionFeedbackPendingResponse;
import com.ktb.interview.application.InterviewSessionManagementService;
import com.ktb.interview.session.domain.InterviewHistoryItem;
import com.ktb.interview.session.domain.InterviewSessionFeedback;
import com.ktb.interview.session.domain.InterviewQuestionSnapshot;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.domain.InterviewSessionStatus;
import com.ktb.interview.session.exception.InterviewSessionInvalidInputException;
import com.ktb.interview.session.exception.InterviewSessionInvalidStateException;
import com.ktb.interview.session.repository.InterviewSessionFeedbackRepository;
import com.ktb.interview.session.service.InterviewSessionService;
import com.ktb.question.domain.Question;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import com.ktb.question.repository.QuestionRepository;
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
    private static final String ERROR_FIRST_QUESTION_NOT_FOUND =
            "failed to generate first question from question pool";

    private final InterviewSessionService interviewSessionService;
    private final InterviewSessionFeedbackRepository feedbackRepository;
    private final QuestionRepository questionRepository;

    /**
     * 연습/실전 모드별 초기 상태를 세팅해 인터뷰 세션을 생성합니다.
     */
    @Override
    public InterviewSessionCreateResponse createSession(Long accountId, InterviewSessionCreateRequest request) {
        log.info("createSession - accountId={}, interviewType={}, questionType={}",
                accountId, request.interviewType(), request.questionType());
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

        QuestionCategory randomCategory = resolveRandomCategoryForQuestionType(session.getQuestionType());
        InterviewQuestionSnapshot firstQuestion = resolveFirstQuestionFromDb(session.getQuestionType(), randomCategory);

        TurnType firstTurnType = TurnType.MAIN;
        Integer firstTopicId = 1;
        session.updateNextQuestion(firstQuestion, firstTurnType, firstTopicId);
        interviewSessionService.save(session);
        log.info("Real session first question prepared from DB - sessionId={}, topicId={}, turnType={}, category={}",
                session.getSessionId(), firstTopicId, firstTurnType, firstQuestion.category());

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
    }

    /**
     * questionType 기준으로 지원되는 카테고리를 랜덤 선택합니다.
     */
    private QuestionCategory resolveRandomCategoryForQuestionType(QuestionType questionType) {
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
     * 랜덤 카테고리/질문타입 기준으로 첫 질문을 DB에서 조회합니다.
     */
    private InterviewQuestionSnapshot resolveFirstQuestionFromDb(QuestionType questionType, QuestionCategory randomCategory) {
        Optional<Question> question = randomCategory == null
                ? Optional.empty()
                : questionRepository.findRandomActiveByTypeAndCategory(questionType.name(), randomCategory.name());
        Question selected = question.orElseGet(() -> questionRepository.findRandomActiveByType(questionType.name())
                .orElseThrow(() -> new InterviewSessionInvalidStateException(ERROR_FIRST_QUESTION_NOT_FOUND)));
        return new InterviewQuestionSnapshot(selected.getId(), selected.getContent(), selected.getCategory());
    }
}
