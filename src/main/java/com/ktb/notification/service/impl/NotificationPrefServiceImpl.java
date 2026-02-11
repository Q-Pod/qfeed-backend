package com.ktb.notification.service.impl;

import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;
import com.ktb.notification.domain.UserNotificationPref;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.dto.response.NotificationPrefResponse;
import com.ktb.notification.repository.UserNotificationPrefRepository;
import com.ktb.notification.service.NotificationPrefService;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPrefServiceImpl implements NotificationPrefService {

    private final UserNotificationPrefRepository prefRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    public List<NotificationPrefResponse> getPreferences(Long accountId) {
        List<UserNotificationPref> preferences = prefRepository.findAllByAccountId(accountId);

        if (preferences.isEmpty()) {
            initializePreferencesInternal(accountId);
            preferences = prefRepository.findAllByAccountId(accountId);
        }

        return preferences.stream()
                .map(NotificationPrefResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public NotificationPrefResponse updatePreference(Long accountId, NotificationTypeCd type, boolean enabled) {
        UserNotificationPref pref = prefRepository.findByAccountIdAndNotificationType(accountId, type)
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
        return prefRepository.findByAccountIdAndNotificationType(accountId, type)
                .map(UserNotificationPref::isEnabled)
                .orElse(true);
    }

    @Transactional
    protected void initializePreferencesInternal(Long accountId) {
        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND) {});

        Arrays.stream(NotificationTypeCd.values())
                .filter(type -> !prefRepository.existsByAccountIdAndNotificationType(accountId, type))
                .forEach(type -> {
                    UserNotificationPref pref = UserNotificationPref.create(account, type);
                    prefRepository.save(pref);
                });
    }

    private UserNotificationPref createPreference(Long accountId, NotificationTypeCd type) {
        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND) {});

        UserNotificationPref pref = UserNotificationPref.create(account, type);
        return prefRepository.save(pref);
    }
}
