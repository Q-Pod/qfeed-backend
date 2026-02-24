package com.ktb.interview.application.service.flow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.interview.session.domain.InterviewHistoryItem;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.domain.InterviewSessionStatus;
import com.ktb.interview.session.dto.response.InterviewFinalFeedbackMetricResponse;
import com.ktb.interview.session.dto.response.InterviewHistoryResponse;
import com.ktb.interview.session.dto.response.InterviewSessionBadCaseFeedbackResponse;
import com.ktb.interview.session.dto.response.InterviewSessionFinalFeedbackResponse;
import com.ktb.interview.session.dto.response.InterviewSessionKeywordResultResponse;
import com.ktb.interview.session.dto.response.InterviewSessionOverallFeedbackResponse;
import com.ktb.interview.session.dto.response.InterviewSessionStateResponse;
import com.ktb.interview.session.dto.response.InterviewSessionTopicFeedbackResponse;
import com.ktb.interview.session.dto.response.SessionFeedbackFailedResponse;
import com.ktb.interview.session.dto.response.SessionFeedbackPendingResponse;
import com.ktb.interview.session.exception.InterviewSessionAccessDeniedException;
import com.ktb.interview.session.exception.InterviewSessionInvalidStateException;
import com.ktb.interview.session.persistence.repository.InterviewSessionEntityRepository;
import com.ktb.interview.session.persistence.repository.projection.InterviewSessionFinalFeedbackReadModel;
import com.ktb.interview.session.service.InterviewSessionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 세션 상태/피드백 조회 유스케이스를 담당합니다.
 * 최종 피드백 조회는 sessionId 기준 단일 DB read model(native + jsonb_agg)로 수행합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewSessionFeedbackQueryFlowService {

    private static final String ERROR_PENDING_STATE_REQUIRED = "session is not in pending state";
    private static final String ERROR_FAILED_STATE_REQUIRED = "session is not failed";
    private static final String ERROR_FEEDBACK_NOT_COMPLETED = "session feedback is not completed yet";
    private static final String ERROR_FINAL_FEEDBACK_JSON_PARSE_FAILED =
            "failed to parse final feedback read model json";

    private final InterviewSessionService interviewSessionService;
    private final InterviewSessionEntityRepository interviewSessionEntityRepository;
    private final ObjectMapper objectMapper;

    /**
     * 세션 현재 상태와 누적 이력을 조회해 응답 모델로 변환합니다.
     * 조회 시 TTL은 연장하지 않습니다.
     */
    public InterviewSessionStateResponse getSessionState(Long accountId, String sessionId) {
        InterviewSession session = interviewSessionService.getSessionWithoutTouch(accountId, sessionId);
        List<InterviewHistoryItem> history = session.getInterviewHistoryView();
        InterviewHistoryItem last = history.isEmpty() ? null : history.getLast();
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
     * 완료된 세션의 최종 피드백을 DB read model 단일 조회로 반환합니다.
     */
    public InterviewSessionFinalFeedbackResponse getSessionFeedbackCompleted(Long accountId, String sessionId) {
        log.debug("getSessionFeedbackCompleted request - accountId={}, sessionId={}", accountId, sessionId);
        InterviewSessionFinalFeedbackReadModel readModel = interviewSessionEntityRepository
                .findFinalFeedbackReadModelBySessionId(sessionId)
                .orElseThrow(() -> new InterviewSessionInvalidStateException(ERROR_FEEDBACK_NOT_COMPLETED));

        if (!readModel.getAccountId().equals(accountId)) {
            log.warn("getSessionFeedbackCompleted access denied - sessionId={}, requestedAccountId={}, ownerAccountId={}",
                    sessionId, accountId, readModel.getAccountId());
            throw new InterviewSessionAccessDeniedException(sessionId, accountId);
        }
        if (!InterviewSessionStatus.COMPLETED.name().equals(readModel.getSessionStatus())) {
            throw new InterviewSessionInvalidStateException(ERROR_FEEDBACK_NOT_COMPLETED);
        }
        if (readModel.getFeedbackSessionId() == null) {
            throw new InterviewSessionInvalidStateException(ERROR_FEEDBACK_NOT_COMPLETED);
        }

        List<InterviewFinalFeedbackMetricResponse> metrics = parseJsonList(
                readModel.getMetricsJson(),
                new TypeReference<>() {
                }
        );
        List<InterviewSessionTopicFeedbackResponse> topicFeedbacks = parseJsonList(
                readModel.getTopicsJson(),
                new TypeReference<>() {
                }
        );
        List<InterviewHistoryResponse> history = parseJsonList(
                readModel.getHistoryJson(),
                new TypeReference<>() {
                }
        );
        List<String> coveredKeywords = parseJsonListNullable(
                readModel.getKeywordCoveredJson(),
                new TypeReference<>() {
                }
        );
        List<String> missingKeywords = parseJsonListNullable(
                readModel.getKeywordMissingJson(),
                new TypeReference<>() {
                }
        );

        InterviewSessionBadCaseFeedbackResponse badCase = readModel.getBadCaseType() == null
                ? null
                : new InterviewSessionBadCaseFeedbackResponse(
                readModel.getBadCaseType(),
                readModel.getBadCaseMessage(),
                readModel.getBadCaseGuidance()
        );

        InterviewSessionKeywordResultResponse keywordResult = coveredKeywords == null
                && missingKeywords == null
                && readModel.getKeywordCoverageRatio() == null
                ? null
                : new InterviewSessionKeywordResultResponse(
                coveredKeywords,
                missingKeywords,
                readModel.getKeywordCoverageRatio() == null
                        ? null
                        : readModel.getKeywordCoverageRatio().doubleValue()
        );

        InterviewSessionOverallFeedbackResponse overallFeedback = readModel.getOverallStrengths() == null
                && readModel.getOverallImprovements() == null
                ? null
                : new InterviewSessionOverallFeedbackResponse(
                readModel.getOverallStrengths(),
                readModel.getOverallImprovements()
        );

        boolean isReal = "REAL_INTERVIEW".equalsIgnoreCase(readModel.getInterviewType());
        Long userId = isReal ? null : readModel.getAccountId();
        Long questionId = isReal ? null : readModel.getInitialQuestionId();
        List<InterviewSessionTopicFeedbackResponse> topicsFeedback = topicFeedbacks.isEmpty() ? null : topicFeedbacks;

        log.info("getSessionFeedbackCompleted via read-model - accountId={}, sessionId={}, metrics={}, topics={}, turns={}",
                accountId, sessionId, metrics.size(), topicFeedbacks.size(), history.size());

        return new InterviewSessionFinalFeedbackResponse(
                null,
                userId,
                questionId,
                readModel.getSessionId(),
                InterviewSessionStatus.COMPLETED.name(),
                badCase,
                metrics,
                keywordResult,
                topicsFeedback,
                overallFeedback,
                history
        );
    }

    private <T> List<T> parseJsonList(String json, TypeReference<List<T>> typeReference) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("getSessionFeedbackCompleted json parse failed - reason={}", e.getOriginalMessage());
            throw new InterviewSessionInvalidStateException(
                    ERROR_FINAL_FEEDBACK_JSON_PARSE_FAILED + ": " + e.getOriginalMessage()
            );
        }
    }

    private <T> List<T> parseJsonListNullable(String json, TypeReference<List<T>> typeReference) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return parseJsonList(json, typeReference);
    }
}
