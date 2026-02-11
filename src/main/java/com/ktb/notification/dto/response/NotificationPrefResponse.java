package com.ktb.notification.dto.response;

import com.ktb.notification.domain.UserNotificationPref;
import com.ktb.notification.domain.enums.NotificationTypeCd;

public record NotificationPrefResponse(
        Long id,
        NotificationTypeCd notificationType,
        String notificationTypeDescription,
        boolean enabled
) {

    public static NotificationPrefResponse from(UserNotificationPref pref) {
        return new NotificationPrefResponse(
                pref.getId(),
                pref.getNotificationType(),
                pref.getNotificationType().getDescription(),
                pref.isEnabled()
        );
    }
}
