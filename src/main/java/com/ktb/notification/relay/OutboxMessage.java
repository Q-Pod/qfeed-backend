package com.ktb.notification.relay;

import com.ktb.notification.domain.NotificationOutbox;
import java.time.Instant;
import java.util.Map;

public record OutboxMessage(
        String messageId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String dedupeKey,
        String traceId,
        Instant occurredAt,
        int retryCount,
        int maxRetries,
        Map<String, Object> payload
) {

    public static OutboxMessage from(NotificationOutbox outbox) {
        return new OutboxMessage(
            outbox.getMessageId(),
            outbox.getEventType(),
            outbox.getAggregateType(),
            outbox.getAggregateId(),
            outbox.getDedupeKey(),
            outbox.getMessageId(),
            outbox.getScheduledAt(),
            outbox.getRetryCount(),
            outbox.getMaxRetries(),
            outbox.getPayloadAsMap()
        );
    }
}
