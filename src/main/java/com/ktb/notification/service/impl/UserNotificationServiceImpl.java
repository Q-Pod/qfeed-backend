package com.ktb.notification.service.impl;

import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;
import com.ktb.notification.domain.Notice;
import com.ktb.notification.domain.UserNotification;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.dto.response.UnreadCountResponse;
import com.ktb.notification.dto.response.UserNotificationResponse;
import com.ktb.notification.exception.UserNotificationNotFoundException;
import com.ktb.notification.repository.NoticeRepository;
import com.ktb.notification.repository.UserNotificationRepository;
import com.ktb.notification.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserNotificationServiceImpl implements UserNotificationService {

    private final UserNotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final NoticeRepository noticeRepository;

    @Override
    public Page<UserNotificationResponse> getNotifications(Long accountId, Pageable pageable) {
        return notificationRepository.findByAccountId(accountId, pageable)
                .map(UserNotificationResponse::from);
    }

    @Override
    public UnreadCountResponse getUnreadCount(Long accountId) {
        long count = notificationRepository.countUnreadByAccountId(accountId);
        return UnreadCountResponse.of(count);
    }

    @Override
    @Transactional
    public UserNotificationResponse markAsRead(Long accountId, Long notificationId) {
        UserNotification notification = notificationRepository.findByIdAndAccountId(notificationId, accountId)
                .orElseThrow(() -> new UserNotificationNotFoundException(notificationId));

        notification.markAsRead();

        return UserNotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(Long accountId) {
        return notificationRepository.markAllAsReadByAccountId(accountId);
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
        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND) {});

        UserNotification notification = UserNotification.create(
                account,
                type,
                title,
                body,
                deeplink,
                referenceId
        );

        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void createNoticeNotification(Long accountId, Long noticeId) {
        if (notificationRepository.existsByAccountIdAndNotificationTypeAndReferenceId(
                accountId, NotificationTypeCd.NOTICE, noticeId)) {
            return;
        }

        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND) {});

        Notice notice = noticeRepository.findActiveById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND) {});

        UserNotification notification = UserNotification.createFromNotice(account, notice);

        notificationRepository.save(notification);
    }
}
