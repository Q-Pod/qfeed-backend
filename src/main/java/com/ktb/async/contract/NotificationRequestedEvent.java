package com.ktb.async.contract;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationRequestedEvent(
        String eventId,
        String eventType,
        Instant eventTime,
        String source,
        Long targetAccountId,
        NotificationType notificationType,
        String notificationCode,
        Map<String, String> templateParams,
        String deepLink,
        Priority priority,
        String traceId,
        String correlationId,
        Integer retryCount
) implements BaseEvent {

    private static final String TYPE = "NOTIFICATION_REQUESTED";

    public static NotificationRequestedEvent create(
            String source,
            String traceId,
            Long targetAccountId,
            NotificationType notificationType,
            String notificationCode,
            Map<String, String> templateParams,
            String deepLink,
            Priority priority,
            String correlationId
    ) {
        return new NotificationRequestedEvent(
                UUID.randomUUID().toString(),
                TYPE,
                Instant.now(),
                source,
                targetAccountId,
                notificationType,
                notificationCode,
                templateParams,
                deepLink,
                priority,
                traceId,
                correlationId,
                0
        );
    }
}
