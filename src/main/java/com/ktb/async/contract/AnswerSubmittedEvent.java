package com.ktb.async.contract;

import com.ktb.answer.domain.Answer;
import com.ktb.answer.domain.AnswerType;
import java.time.Instant;
import java.util.UUID;

public record AnswerSubmittedEvent(
        String eventId,
        String eventType,
        Instant eventTime,
        String source,
        Long answerId,
        Long accountId,
        Long questionId,
        String sessionId,
        String answerContent,
        AnswerType answerType,
        String audioUrl,
        String videoUrl,
        String traceId,
        Integer retryCount
) implements BaseEvent {

    private static final String SOURCE = "answer-service";
    private static final String TYPE = "ANSWER_SUBMITTED";

    public static AnswerSubmittedEvent create(Answer answer, String traceId) {
        return new AnswerSubmittedEvent(
                UUID.randomUUID().toString(),
                TYPE,
                Instant.now(),
                SOURCE,
                answer.getId(),
                answer.getAccount().getId(),
                answer.getQuestion().getId(),
                answer.getSessionId(),
                answer.getContent(),
                answer.getType(),
                null,   // audioUrl — MVP V2에서 구현
                null,   // videoUrl — MVP V2에서 구현
                traceId,
                0
        );
    }

    public AnswerSubmittedEvent withRetryCount(Integer newRetryCount) {
        return new AnswerSubmittedEvent(
                eventId, eventType, eventTime, source,
                answerId, accountId, questionId, sessionId,
                answerContent, answerType, audioUrl, videoUrl,
                traceId, newRetryCount
        );
    }
}
