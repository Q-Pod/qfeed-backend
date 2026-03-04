package com.ktb.notification.service;

import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.dto.response.NotificationPrefResponse;
import java.util.List;

public interface NotificationPrefService {

    List<NotificationPrefResponse> getPreferences(Long accountId);

    NotificationPrefResponse updatePreference(Long accountId, NotificationTypeCd type, boolean enabled);

    void initializePreferences(Long accountId);

    boolean isNotificationEnabled(Long accountId, NotificationTypeCd type);

    void insertDefaultPreferencesForAllActiveUsers();
}
