package com.ktb.async.contract;

import com.ktb.answer.domain.Answer;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record FeedbackCompletedEvent(
        String eventId,
        String eventType,
        Instant eventTime,
        String deepLink,
        String source,
        Long answerId,
        Long accountId,
        Map<String, Integer> metrics,
        String overallComment,
        String strengths,
        String improvements,
        String traceId,
        String correlationId,
        Integer retryCount
) implements BaseEvent {

    private static final String SOURCE = "feedback-worker";
    private static final String TYPE = "FEEDBACK_COMPLETED";

    public static FeedbackCompletedEvent create(
            Answer answer,
            Map<String, Integer> metrics,
            String overallComment,
            String strengths,
            String improvements,
            String traceId,
            String correlationId
    ) {
        return new FeedbackCompletedEvent(
                UUID.randomUUID().toString(),
                TYPE,
                Instant.now(),
                answer.getType().getDeepLink(),
                SOURCE,
                answer.getId(),
                answer.getAccount().getId(),
                metrics,
                overallComment,
                strengths,
                improvements,
                traceId,
                correlationId,
                0
        );
    }
}
