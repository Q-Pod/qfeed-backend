package com.ktb.notification.service.impl;

import com.ktb.auth.domain.AccountStatus;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.domain.enums.TargetUsersType;
import com.ktb.notification.repository.UserDeviceRepository;
import com.ktb.notification.repository.UserNotificationPrefRepository;
import com.ktb.notification.service.TargetUserResolver;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TargetUserResolverImpl implements TargetUserResolver {

    private final UserAccountRepository userAccountRepository;
    private final UserNotificationPrefRepository prefRepository;
    private final UserDeviceRepository userDeviceRepository;

    @Override
    public List<Long> resolveAllActiveUsers() {
        return userAccountRepository.findAllActiveAccountIds(AccountStatus.ACTIVE);
    }

    @Override
    public List<Long> filterByNotificationPref(List<Long> accountIds, NotificationTypeCd type) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }

        List<Long> disabledAccountIds = prefRepository
                .findDisabledAccountIdsByTypeAndAccountIds(type, accountIds);

        Set<Long> disabledSet = new HashSet<>(disabledAccountIds);

        return accountIds.stream()
                .filter(id -> !disabledSet.contains(id))
                .toList();
    }

    @Override
    public List<Long> filterByActiveDevice(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }

        return accountIds.stream()
                .filter(accountId -> userDeviceRepository.countActiveByAccountId(accountId) > 0)
                .toList();
    }

    @Override
    public List<Long> resolveTargets(NotificationTypeCd type) {
        return resolveTargets(type, TargetUsersType.ALL);
    }

    @Override
    public List<Long> resolveTargets(NotificationTypeCd type, TargetUsersType targetUsers) {
        // TODO: INACTIVE_D3 — 최근 3일간 비활성 사용자 쿼리 구현 필요
        List<Long> baseUsers = switch (targetUsers) {
            case ALL, ACTIVE, INACTIVE_D3 -> resolveAllActiveUsers();
        };

        List<Long> filteredByPref = filterByNotificationPref(baseUsers, type);

        return filterByActiveDevice(filteredByPref);
    }
}
