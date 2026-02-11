package com.ktb.notification.service.impl;

import com.ktb.notification.domain.UserNotification;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.repository.UserNotificationRepository;
import com.ktb.notification.service.UserNotificationStore;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JpaUserNotificationStore implements UserNotificationStore {

    private final UserNotificationRepository repository;

    @Override
    public Page<UserNotification> findByAccountId(Long accountId, Pageable pageable) {
        return repository.findByAccountId(accountId, pageable);
    }

    @Override
    public long countUnreadByAccountId(Long accountId) {
        return repository.countUnreadByAccountId(accountId);
    }

    @Override
    public Optional<UserNotification> findByIdAndAccountId(Long id, Long accountId) {
        return repository.findByIdAndAccountId(id, accountId);
    }

    @Override
    public int markAllAsReadByAccountId(Long accountId) {
        return repository.markAllAsReadByAccountId(accountId);
    }

    @Override
    public boolean existsByAccountIdAndTypeAndReferenceId(Long accountId, NotificationTypeCd type, Long referenceId) {
        return repository.existsByAccountIdAndNotificationTypeAndReferenceId(accountId, type, referenceId);
    }

    @Override
    public UserNotification save(UserNotification notification) {
        return repository.save(notification);
    }
}
