package com.ktb.notification.dto.response;

import com.ktb.notification.domain.UserNotification;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import java.time.LocalDateTime;

public record UserNotificationResponse(
        Long id,
        NotificationTypeCd notificationType,
        String notificationTypeDescription,
        String title,
        String body,
        String deeplink,
        Long referenceId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {

    public static UserNotificationResponse from(UserNotification notification) {
        return new UserNotificationResponse(
                notification.getId(),
                notification.getNotificationType(),
                notification.getNotificationType().getDescription(),
                notification.getTitle(),
                notification.getBody(),
                notification.getDeeplink(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
