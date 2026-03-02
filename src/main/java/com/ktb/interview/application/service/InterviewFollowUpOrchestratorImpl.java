package com.ktb.interview.application.service;

import com.ktb.ai.feedback.exception.AiFeedbackRequestRejectedException;
import com.ktb.ai.feedback.exception.AiFeedbackRetryableException;
import com.ktb.ai.feedback.exception.AiFeedbackServiceTemporarilyUnavailableException;
import com.ktb.ai.feedback.exception.AiFeedbackTimeoutException;
import com.ktb.ai.feedback.exception.AiFeedbackDependencyFailedException;
import com.ktb.answer.domain.TurnType;
import com.ktb.interview.dto.ai.InterviewFollowUpQuestionApiResponse;
import com.ktb.interview.dto.ai.InterviewFollowUpQuestionDataResponse;
import com.ktb.interview.dto.ai.InterviewFollowUpQuestionRequest;
import com.ktb.interview.dto.ai.InterviewHistoryRequest;
import com.ktb.interview.port.out.AiInterviewPort;
import com.ktb.interview.application.InterviewFollowUpOrchestrator;
import com.ktb.interview.session.domain.InterviewHistoryItem;
import com.ktb.interview.session.domain.InterviewSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 세션 이력을 기반으로 다음 질문/종료를 결정하는 구현체.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewFollowUpOrchestratorImpl implements InterviewFollowUpOrchestrator {

    private static final String ERROR_FOLLOW_UP_RESPONSE_EMPTY = "AI follow-up response is empty";
    private static final String ERROR_FOLLOW_UP_EMPTY_KEYWORD = "response is empty";
    private static final String ERROR_TIMEOUT_KEYWORD_1 = "timed out";
    private static final String ERROR_TIMEOUT_KEYWORD_2 = "timeout";

    private final AiInterviewPort aiInterviewClient;

    /**
     * 세션 이력을 기반으로 다음 질문/종료 여부를 AI 서버에서 결정합니다.
     */
    @Override
    public FollowUpDecision decideNext(
            InterviewSession session,
            String questionTypeOverride,
            List<InterviewHistoryRequest> historyOverride
    ) {
        log.debug("decideNext start - sessionId={}, questionTypeOverride={}, overrideHistorySize={}",
                session.getSessionId(),
                questionTypeOverride,
                historyOverride == null ? 0 : historyOverride.size());
        InterviewFollowUpQuestionRequest request = new InterviewFollowUpQuestionRequest(
                session.getAccountId(),
                session.getSessionId(),
                resolveQuestionType(session, questionTypeOverride),
                resolveFollowUpHistory(session, historyOverride)
        );

        try {
            InterviewFollowUpQuestionApiResponse response = aiInterviewClient.requestFollowUpQuestion(request);
            if (response == null || response.data() == null) {
                throw new AiFeedbackServiceTemporarilyUnavailableException(ERROR_FOLLOW_UP_RESPONSE_EMPTY);
            }
            InterviewFollowUpQuestionDataResponse data = response.data();

            boolean shouldEnd = "session_ended".equalsIgnoreCase(response.message())
                    || Boolean.TRUE.equals(data.isSessionEnded());
            boolean isBadCase = "bad_case_detected".equalsIgnoreCase(response.message())
                    || Boolean.TRUE.equals(data.isBadCase());
            log.info("decideNext result - sessionId={}, message={}, shouldEnd={}, badCase={}, topicId={}, turnType={}",
                    session.getSessionId(), response.message(), shouldEnd, isBadCase, data.topicId(), data.turnType());

            return new FollowUpDecision(
                    response.message(),
                    shouldEnd,
                    isBadCase,
                    data.badCaseFeedback(),
                    data.endReason(),
                    data.turnType(),
                    data.topicId(),
                    data.questionText(),
                    data.category()
            );
        } catch (AiFeedbackRequestRejectedException e) {
            log.warn("AI follow-up request rejected - sessionId={}, reason={}",
                    session.getSessionId(), e.getMessage());
            throw e;
        } catch (AiFeedbackRetryableException e) {
            RuntimeException mappedException = mapRetryableException(e);
            log.warn("AI follow-up decision failed - sessionId={}, mappedException={}, reason={}",
                    session.getSessionId(), mappedException.getClass().getSimpleName(), e.getMessage());
            throw mappedException;
        }
    }

    private RuntimeException mapRetryableException(AiFeedbackRetryableException e) {
        String reason = e.getMessage();
        if (isTimeout(reason)) {
            return new AiFeedbackTimeoutException(reason, e);
        }
        if (isEmptyResponse(reason)) {
            return new AiFeedbackServiceTemporarilyUnavailableException(reason, e);
        }
        return new AiFeedbackDependencyFailedException(reason, e);
    }

    private boolean isTimeout(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains(ERROR_TIMEOUT_KEYWORD_1) || normalized.contains(ERROR_TIMEOUT_KEYWORD_2);
    }

    private boolean isEmptyResponse(String message) {
        if (message == null) {
            return false;
        }
        return message.toLowerCase().contains(ERROR_FOLLOW_UP_EMPTY_KEYWORD);
    }

    /**
     * 도메인 이력 모델을 follow-up API 요청 이력 모델로 변환합니다.
     */
    private List<InterviewHistoryRequest> toFollowUpHistory(List<InterviewHistoryItem> history) {
        return history.stream()
                .map(item -> new InterviewHistoryRequest(
                        item.question(),
                        item.answerText(),
                        normalizeFollowUpTurnType(item.turnType()),
                        item.turnOrder(),
                        item.topicId(),
                        item.category()
                ))
                .toList();
    }

    /**
     * 외부 요청 히스토리가 있으면 그대로 사용하고, 없으면 세션 누적 이력을 사용합니다.
     */
    private List<InterviewHistoryRequest> resolveFollowUpHistory(
            InterviewSession session,
            List<InterviewHistoryRequest> historyOverride
    ) {
        if (historyOverride != null && !historyOverride.isEmpty()) {
            return historyOverride.stream()
                    .map(item -> new InterviewHistoryRequest(
                            item.question(),
                            item.answerText(),
                            item.turnType(),
                            item.turnOrder(),
                            item.topicId(),
                            item.category()
                    ))
                    .toList();
        }
        return toFollowUpHistory(session.getInterviewHistoryView());
    }

    /**
     * follow-up API 스키마에 맞게 MAIN turn 을 new_topic 값으로 정규화합니다.
     */
    private String normalizeFollowUpTurnType(TurnType turnType) {
        if (turnType == TurnType.MAIN) {
            return TurnType.NEW_TOPIC.wireValue();
        }
        return turnType.wireValue();
    }

    /**
     * question_type 오버라이드가 있으면 우선 사용하고 없으면 세션 questionType을 사용합니다.
     */
    private String resolveQuestionType(InterviewSession session, String questionTypeOverride) {
        if (questionTypeOverride != null && !questionTypeOverride.isBlank()) {
            return questionTypeOverride.trim();
        }
        return session.getQuestionType().name();
    }
}
