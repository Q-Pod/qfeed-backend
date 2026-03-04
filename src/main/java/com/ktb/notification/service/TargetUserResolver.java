package com.ktb.notification.service;

import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.domain.enums.TargetUsersType;
import java.util.List;

public interface TargetUserResolver {

    List<Long> resolveAllActiveUsers();

    List<Long> filterByNotificationPref(List<Long> accountIds, NotificationTypeCd type);

    List<Long> filterByActiveDevice(List<Long> accountIds);

    List<Long> resolveTargets(NotificationTypeCd type);

    List<Long> resolveTargets(NotificationTypeCd type, TargetUsersType targetUsers);
}
