package com.ktb.interview.application.service;

import com.ktb.ai.feedback.exception.AiFeedbackDependencyFailedException;
import com.ktb.ai.feedback.exception.AiFeedbackRequestRejectedException;
import com.ktb.ai.feedback.exception.AiFeedbackRetryableException;
import com.ktb.answer.domain.Answer;
import com.ktb.answer.domain.AnswerStatus;
import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.domain.TurnType;
import com.ktb.interview.dto.ai.InterviewFeedbackApiResponse;
import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.dto.ai.InterviewFeedbackRequest;
import com.ktb.interview.dto.ai.InterviewHistoryRequest;
import com.ktb.interview.dto.ai.InterviewKeywordResultResponse;
import com.ktb.answer.repository.AnswerRepository;
import com.ktb.interview.port.out.AiInterviewPort;
import com.ktb.answer.service.AnswerDomainService;
import com.ktb.interview.application.SessionFeedbackOrchestrator;
import com.ktb.interview.session.domain.InterviewHistoryItem;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.service.InterviewSessionService;
import com.ktb.common.domain.ErrorCode;
import com.ktb.hashtag.domain.QuestionHashtag;
import com.ktb.question.domain.Question;
import com.ktb.question.domain.QuestionCategory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 세션 이력을 기반으로 AI 최종 피드백 요청/재시도를 수행하는 구현체.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionFeedbackOrchestratorImpl implements SessionFeedbackOrchestrator {

    private static final int[] RETRY_DELAYS_SECONDS = {1, 2, 4};
    private static final String ERROR_INTERRUPTED_WHILE_RETRY_WAIT =
            "Interrupted while waiting AI feedback retry";
    private static final String ERROR_RETRY_EXHAUSTED =
            "AI feedback dependency failed after retry exhaustion";

    private final AnswerRepository answerRepository;
    private final AnswerDomainService answerDomainService;
    private final InterviewSessionService interviewSessionService;
    private final AiInterviewPort aiInterviewClient;

    /**
     * 세션 이력/키워드를 AI 요청 포맷으로 변환해 피드백 생성 파이프라인을 실행합니다.
     */
    @Override
    public InterviewFeedbackDataResponse generateFeedback(
            Long accountId,
            InterviewSession session,
            Question question,
            Answer answer,
            List<InterviewHistoryItem> history,
            List<QuestionHashtag> questionHashtags
    ) {
        log.info("generateFeedback - accountId={}, sessionId={}, answerId={}, interviewType={}, historySize={}, hashtagSize={}",
                accountId, session.getSessionId(), answer.getId(), session.getInterviewType(), history.size(),
                questionHashtags == null ? 0 : questionHashtags.size());
        List<String> keywords = resolveKeywordsForRequest();
        QuestionCategory historyCategory = question.getCategory();
        String aiCategory = session.getInterviewType() == AnswerType.REAL_INTERVIEW && historyCategory != null
                ? toAiCategory(historyCategory.name())
                : null;
        InterviewFeedbackRequest aiRequest = new InterviewFeedbackRequest(
                accountId,
                question.getId(),
                session.getSessionId(),
                session.getInterviewType().name(),
                session.getQuestionType().name(),
                aiCategory,
                toAiHistory(history, session.getInterviewType(), historyCategory),
                keywords
        );

        try {
            InterviewFeedbackApiResponse aiResponse = requestFeedbackWithRetry(session, answer, aiRequest);
            log.info("generateFeedback success - sessionId={}, answerId={}, aiMessage={}",
                    session.getSessionId(), answer.getId(), aiResponse.message());
            return normalizeFeedbackData(
                    aiResponse.data(),
                    answer.getId(),
                    accountId,
                    question.getId(),
                    session.getSessionId()
            );
        } catch (AiFeedbackRequestRejectedException e) {
            answerDomainService.transitionStatus(answer, AnswerStatus.FAILED);
            answerRepository.save(answer);

            session.markFailed(ErrorCode.INVALID_INPUT.getCode(), e.getMessage(), session.getRetryCount());
            interviewSessionService.save(session);
            log.warn("generateFeedback rejected - sessionId={}, answerId={}, reason={}",
                    session.getSessionId(), answer.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 재시도 정책에 따라 AI 피드백 요청을 수행합니다.
     */
    private InterviewFeedbackApiResponse requestFeedbackWithRetry(
            InterviewSession session,
            Answer answer,
            InterviewFeedbackRequest request
    ) {
        answerDomainService.transitionStatus(answer, AnswerStatus.AI_FEEDBACK_PROCESSING);
        answerRepository.save(answer);

        int totalAttempts = RETRY_DELAYS_SECONDS.length + 1;

        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                log.debug("requestFeedbackWithRetry attempt - sessionId={}, answerId={}, attempt={}/{}",
                        session.getSessionId(), answer.getId(), attempt, totalAttempts);
                InterviewFeedbackApiResponse response = aiInterviewClient.requestFeedback(request);
                session.markInProgress();
                interviewSessionService.save(session);
                log.debug("requestFeedbackWithRetry success - sessionId={}, answerId={}, attempt={}",
                        session.getSessionId(), answer.getId(), attempt);
                return response;
            } catch (AiFeedbackRequestRejectedException e) {
                throw e;
            } catch (AiFeedbackRetryableException e) {
                boolean finalAttempt = attempt == totalAttempts;
                if (finalAttempt) {
                    answerDomainService.transitionStatus(answer, AnswerStatus.FAILED);
                    answerRepository.save(answer);

                    String failureReason = abbreviateErrorMessage(e.getMessage());
                    session.markFailed(
                            ErrorCode.AI_FEEDBACK_DEPENDENCY_FAILED.getCode(),
                            failureReason,
                            RETRY_DELAYS_SECONDS.length
                    );
                    interviewSessionService.save(session);
                    log.error("requestFeedbackWithRetry exhausted - sessionId={}, answerId={}, attempts={}, reason={}",
                            session.getSessionId(), answer.getId(), totalAttempts, failureReason);

                    throw new AiFeedbackDependencyFailedException(failureReason, e);
                }

                int retryCount = attempt;
                int delaySeconds = RETRY_DELAYS_SECONDS[attempt - 1];
                LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(delaySeconds);
                log.warn("requestFeedbackWithRetry retryable error - sessionId={}, answerId={}, attempt={}/{}, nextRetryAt={}, reason={}",
                        session.getSessionId(), answer.getId(), attempt, totalAttempts, nextRetryAt, e.getMessage());

                answerDomainService.transitionStatus(answer, AnswerStatus.FAILED_RETRYABLE);
                answerRepository.save(answer);

                session.markRetrying(retryCount, nextRetryAt);
                interviewSessionService.save(session);

                try {
                    Thread.sleep(delaySeconds * 1000L);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new AiFeedbackDependencyFailedException(
                            ERROR_INTERRUPTED_WHILE_RETRY_WAIT,
                            interruptedException
                    );
                }

                answerDomainService.transitionStatus(answer, AnswerStatus.AI_FEEDBACK_PROCESSING);
                answerRepository.save(answer);
            }
        }

        throw new AiFeedbackDependencyFailedException(ERROR_RETRY_EXHAUSTED);
    }

    /**
     * 클라이언트/세션 로그에 저장할 오류 메시지 길이를 제한합니다.
     */
    private String abbreviateErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return ErrorCode.AI_FEEDBACK_DEPENDENCY_FAILED.getMessage();
        }
        int maxLength = 500;
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...";
    }

    /**
     * 인터뷰 유형별로 AI 요청에 전달할 이력 범위를 결정합니다.
     */
    private List<InterviewHistoryRequest> toAiHistory(
            List<InterviewHistoryItem> history,
            AnswerType interviewType,
            QuestionCategory fallbackCategory
    ) {
        if (interviewType == AnswerType.PRACTICE_INTERVIEW) {
            InterviewHistoryItem latest = history.get(history.size() - 1);
            return List.of(toAiHistoryItem(latest, fallbackCategory));
        }
        return history.stream()
                .map(item -> toAiHistoryItem(item, fallbackCategory))
                .toList();
    }

    /**
     * 인터뷰 유형별 키워드 전달 정책을 적용합니다.
     */
    private List<String> resolveKeywordsForRequest() {
        return null;
    }

    /**
     * 세션 turn 이력 도메인 모델을 AI 요청 스키마로 매핑합니다.
     */
    private InterviewHistoryRequest toAiHistoryItem(InterviewHistoryItem item, QuestionCategory fallbackCategory) {
        QuestionCategory historyCategory = item.category() == null ? fallbackCategory : item.category();
        return new InterviewHistoryRequest(
                item.question(),
                item.answerText(),
                normalizeFeedbackTurnType(item.turnType()),
                item.turnOrder(),
                item.topicId(),
                historyCategory
        );
    }

    /**
     * 피드백 API 스키마(new_topic/follow_up)에 맞춰 turn type을 정규화합니다.
     */
    private String normalizeFeedbackTurnType(TurnType turnType) {
        if (turnType == TurnType.MAIN) {
            return TurnType.NEW_TOPIC.wireValue();
        }
        return turnType.wireValue();
    }

    /**
     * AI 서버 카테고리 계약과 내부 카테고리 차이를 매핑합니다.
     */
    private String toAiCategory(String category) {
        return category;
    }

    /**
     * AI 응답의 누락 필드를 기본값으로 보정해 내부 응답 모델로 정규화합니다.
     */
    private InterviewFeedbackDataResponse normalizeFeedbackData(
            InterviewFeedbackDataResponse data,
            Long answerId,
            Long userId,
            Long questionId,
            String sessionId
    ) {
        return new InterviewFeedbackDataResponse(
                answerId,
                data.userId() == null ? userId : data.userId(),
                data.questionId() == null ? questionId : data.questionId(),
                data.sessionId() == null ? sessionId : data.sessionId(),
                data.status(),
                data.badCaseFeedback(),
                data.metrics() == null ? List.of() : data.metrics(),
                data.keywordResult() == null
                        ? new InterviewKeywordResultResponse(List.of(), List.of(), 0.0)
                        : data.keywordResult(),
                data.topicsFeedback(),
                data.overallFeedback(),
                data.nextQuestion(),
                data.nextTurnType(),
                data.nextTopicId(),
                data.isFinal()
        );
    }
}
