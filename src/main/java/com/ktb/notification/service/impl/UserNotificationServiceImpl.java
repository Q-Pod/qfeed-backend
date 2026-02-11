package com.ktb.notification.service.impl;

import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.service.UserAccountService;
import com.ktb.notification.domain.Notice;
import com.ktb.notification.domain.UserNotification;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.dto.response.UnreadCountResponse;
import com.ktb.notification.dto.response.UserNotificationResponse;
import com.ktb.notification.exception.NoticeNotFoundException;
import com.ktb.notification.exception.UserNotificationNotFoundException;
import com.ktb.notification.service.NoticeStore;
import com.ktb.notification.service.UserNotificationService;
import com.ktb.notification.service.UserNotificationStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserNotificationServiceImpl implements UserNotificationService {

    private final UserNotificationStore notificationStore;
    private final UserAccountService userAccountService;
    private final NoticeStore noticeStore;

    @Override
    public Page<UserNotificationResponse> getNotifications(Long accountId, Pageable pageable) {
        return notificationStore.findByAccountId(accountId, pageable)
                .map(UserNotificationResponse::from);
    }

    @Override
    public UnreadCountResponse getUnreadCount(Long accountId) {
        long count = notificationStore.countUnreadByAccountId(accountId);
        return UnreadCountResponse.of(count);
    }

    @Override
    @Transactional
    public UserNotificationResponse markAsRead(Long accountId, Long notificationId) {
        UserNotification notification = notificationStore.findByIdAndAccountId(notificationId, accountId)
                .orElseThrow(() -> new UserNotificationNotFoundException(notificationId));

        notification.markAsRead();

        return UserNotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(Long accountId) {
        return notificationStore.markAllAsReadByAccountId(accountId);
    }

    @Override
    @Transactional
    public void createNotification(
            Long accountId,
            NotificationTypeCd type,
            String title,
            String body,
            String deeplink,
            Long referenceId
    ) {
        UserAccount account = userAccountService.findById(accountId);

        UserNotification notification = UserNotification.create(
                account,
                type,
                title,
                body,
                deeplink,
                referenceId
        );

        notificationStore.save(notification);
    }

    @Override
    @Transactional
    public void createNoticeNotification(Long accountId, Long noticeId) {
        if (notificationStore.existsByAccountIdAndTypeAndReferenceId(
                accountId, NotificationTypeCd.NOTICE, noticeId)) {
            return;
        }

        UserAccount account = userAccountService.findById(accountId);

        Notice notice = noticeStore.findActiveById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));

        UserNotification notification = UserNotification.createFromNotice(account, notice);

        notificationStore.save(notification);
    }
}
