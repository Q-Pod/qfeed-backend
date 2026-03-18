package com.ktb.notification.service;

import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.dto.response.UserNotificationListResponse;
import com.ktb.notification.dto.response.UserNotificationResponse;

public interface UserNotificationService {

    UserNotificationListResponse getNotifications(Long accountId, Long cursor, int size);

    boolean hasUnread(Long accountId);

    UserNotificationResponse markAsRead(Long accountId, Long notificationId);

    int markAllAsRead(Long accountId);

    void createNotification(
            Long accountId,
            NotificationTypeCd type,
            String title,
            String body,
            String deeplink,
            Long referenceId
    );

    void createNoticeNotification(Long accountId, Long noticeId);
}
