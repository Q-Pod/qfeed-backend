package com.ktb.notification.service;

import com.ktb.notification.domain.UserNotificationPref;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import java.util.List;
import java.util.Optional;

public interface NotificationPrefStore {

    List<UserNotificationPref> findAllByAccountId(Long accountId);

    Optional<UserNotificationPref> findByAccountIdAndType(Long accountId, NotificationTypeCd type);

    boolean existsByAccountIdAndType(Long accountId, NotificationTypeCd type);

    UserNotificationPref save(UserNotificationPref pref);
}
