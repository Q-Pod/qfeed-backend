package com.ktb.notification.service.impl;

import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.service.UserAccountService;
import com.ktb.notification.domain.Notice;
import com.ktb.notification.domain.UserNotification;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.dto.response.UserNotificationListResponse;
import com.ktb.notification.dto.response.UserNotificationPaginationResponse;
import com.ktb.notification.dto.response.UserNotificationResponse;
import com.ktb.notification.exception.NoticeNotFoundException;
import com.ktb.notification.exception.UserNotificationNotFoundException;
import com.ktb.notification.repository.NoticeRepository;
import com.ktb.notification.repository.UserNotificationRepository;
import com.ktb.notification.service.UserNotificationService;
import com.ktb.notification.sse.UnreadEventPublisher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserNotificationServiceImpl implements UserNotificationService {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    private final UserNotificationRepository userNotificationRepository;
    private final UserAccountService userAccountService;
    private final NoticeRepository noticeRepository;
    private final UnreadEventPublisher unreadEventPublisher;

    @Override
    public UserNotificationListResponse getNotifications(Long accountId, Long cursor, int size) {
        int validatedSize = Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));
        PageRequest pageRequest = PageRequest.of(0, validatedSize);

        Slice<UserNotification> slice = (cursor == null)
                ? userNotificationRepository.findByAccountIdOrderByIdDesc(accountId, pageRequest)
                : userNotificationRepository.findByAccountIdAndIdLessThanOrderByIdDesc(accountId, cursor, pageRequest);

        List<UserNotificationResponse> notifications = slice.getContent().stream()
                .map(UserNotificationResponse::from)
                .toList();

        Long nextCursor = slice.hasNext()
                ? slice.getContent().get(slice.getContent().size() - 1).getId()
                : null;

        return new UserNotificationListResponse(
                notifications,
                new UserNotificationPaginationResponse(nextCursor, slice.hasNext(), validatedSize)
        );
    }

    @Override
    public boolean hasUnread(Long accountId) {
        return userNotificationRepository.countUnreadByAccountId(accountId) > 0;
    }

    @Override
    @Transactional
    public UserNotificationResponse markAsRead(Long accountId, Long notificationId) {
        UserNotification notification = userNotificationRepository
                .findByIdAndAccountId(notificationId, accountId)
                .orElseThrow(() -> new UserNotificationNotFoundException(notificationId));

        notification.markAsRead();

        boolean hasUnread = userNotificationRepository.countUnreadByAccountId(accountId) > 0;
        unreadEventPublisher.publish(accountId, hasUnread);

        return UserNotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(Long accountId) {
        int count = userNotificationRepository.markAllAsReadByAccountId(accountId);
        unreadEventPublisher.publish(accountId, false);
        return count;
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

        userNotificationRepository.save(notification);
        unreadEventPublisher.publish(accountId, true);
    }

    @Override
    @Transactional
    public void createNoticeNotification(Long accountId, Long noticeId) {
        if (userNotificationRepository.existsByAccountIdAndNotificationTypeAndReferenceId(
                accountId, NotificationTypeCd.NOTICE, noticeId)) {
            return;
        }

        UserAccount account = userAccountService.findById(accountId);

        Notice notice = noticeRepository.findActiveById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));

        UserNotification notification = UserNotification.createFromNotice(account, notice);

        userNotificationRepository.save(notification);
        unreadEventPublisher.publish(accountId, true);
    }
}
