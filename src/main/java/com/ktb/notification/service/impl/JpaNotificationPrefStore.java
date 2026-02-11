package com.ktb.notification.service.impl;

import com.ktb.notification.domain.UserNotificationPref;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.repository.UserNotificationPrefRepository;
import com.ktb.notification.service.NotificationPrefStore;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JpaNotificationPrefStore implements NotificationPrefStore {

    private final UserNotificationPrefRepository repository;

    @Override
    public List<UserNotificationPref> findAllByAccountId(Long accountId) {
        return repository.findAllByAccountId(accountId);
    }

    @Override
    public Optional<UserNotificationPref> findByAccountIdAndType(Long accountId, NotificationTypeCd type) {
        return repository.findByAccountIdAndNotificationType(accountId, type);
    }

    @Override
    public boolean existsByAccountIdAndType(Long accountId, NotificationTypeCd type) {
        return repository.existsByAccountIdAndNotificationType(accountId, type);
    }

    @Override
    public UserNotificationPref save(UserNotificationPref pref) {
        return repository.save(pref);
    }
}
