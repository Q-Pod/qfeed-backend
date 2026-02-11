package com.ktb.notification.service;

import com.ktb.notification.domain.UserDevice;
import com.ktb.notification.domain.enums.DevicePlatformCd;
import java.util.List;
import java.util.Optional;

public interface UserDeviceStore {

    UserDevice save(UserDevice device);

    Optional<UserDevice> findById(Long id);

    List<UserDevice> findActiveByAccountId(Long accountId);

    List<UserDevice> findByAccountId(Long accountId);

    Optional<UserDevice> findActiveByPushToken(String pushToken);

    Optional<UserDevice> findActiveByPushEndpoint(String endpoint);

    List<UserDevice> findActiveByAccountIdAndPlatform(Long accountId, DevicePlatformCd platform);

    boolean existsByPushToken(String pushToken);

    boolean existsByPushEndpoint(String pushEndpoint);

    long countActiveByAccountId(Long accountId);

    void delete(UserDevice device);
}
