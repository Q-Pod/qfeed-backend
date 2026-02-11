package com.ktb.notification.service.impl;

import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.service.UserAccountService;
import com.ktb.notification.domain.UserNotificationPref;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.dto.response.NotificationPrefResponse;
import com.ktb.notification.service.NotificationPrefService;
import com.ktb.notification.service.NotificationPrefStore;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPrefServiceImpl implements NotificationPrefService {

    private final NotificationPrefStore prefStore;
    private final UserAccountService userAccountService;

    @Override
    public List<NotificationPrefResponse> getPreferences(Long accountId) {
        List<UserNotificationPref> preferences = prefStore.findAllByAccountId(accountId);

        if (preferences.isEmpty()) {
            initializePreferencesInternal(accountId);
            preferences = prefStore.findAllByAccountId(accountId);
        }

        return preferences.stream()
                .map(NotificationPrefResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public NotificationPrefResponse updatePreference(Long accountId, NotificationTypeCd type, boolean enabled) {
        UserNotificationPref pref = prefStore.findByAccountIdAndType(accountId, type)
                .orElseGet(() -> createPreference(accountId, type));

        pref.updateEnabled(enabled);

        return NotificationPrefResponse.from(pref);
    }

    @Override
    @Transactional
    public void initializePreferences(Long accountId) {
        initializePreferencesInternal(accountId);
    }

    @Override
    public boolean isNotificationEnabled(Long accountId, NotificationTypeCd type) {
        return prefStore.findByAccountIdAndType(accountId, type)
                .map(UserNotificationPref::isEnabled)
                .orElse(true);
    }

    @Transactional
    protected void initializePreferencesInternal(Long accountId) {
        UserAccount account = userAccountService.findById(accountId);

        Arrays.stream(NotificationTypeCd.values())
                .filter(type -> !prefStore.existsByAccountIdAndType(accountId, type))
                .forEach(type -> {
                    UserNotificationPref pref = UserNotificationPref.create(account, type);
                    prefStore.save(pref);
                });
    }

    private UserNotificationPref createPreference(Long accountId, NotificationTypeCd type) {
        UserAccount account = userAccountService.findById(accountId);

        UserNotificationPref pref = UserNotificationPref.create(account, type);
        return prefStore.save(pref);
    }
}
