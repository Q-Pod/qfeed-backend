package com.ktb.notification.service;

import com.ktb.notification.domain.UserNotification;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserNotificationStore {

    Page<UserNotification> findByAccountId(Long accountId, Pageable pageable);

    long countUnreadByAccountId(Long accountId);

    Optional<UserNotification> findByIdAndAccountId(Long id, Long accountId);

    int markAllAsReadByAccountId(Long accountId);

    boolean existsByAccountIdAndTypeAndReferenceId(Long accountId, NotificationTypeCd type, Long referenceId);

    UserNotification save(UserNotification notification);
}
