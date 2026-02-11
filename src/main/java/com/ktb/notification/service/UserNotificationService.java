package com.ktb.notification.service;

import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.dto.response.UnreadCountResponse;
import com.ktb.notification.dto.response.UserNotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserNotificationService {

    Page<UserNotificationResponse> getNotifications(Long accountId, Pageable pageable);

    UnreadCountResponse getUnreadCount(Long accountId);

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
