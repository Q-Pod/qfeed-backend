package com.ktb.notification.service.impl;

import com.ktb.notification.domain.UserDevice;
import com.ktb.notification.domain.enums.DevicePlatformCd;
import com.ktb.notification.repository.UserDeviceRepository;
import com.ktb.notification.service.UserDeviceStore;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JpaUserDeviceStore implements UserDeviceStore {

    private final UserDeviceRepository repository;

    @Override
    public UserDevice save(UserDevice device) {
        return repository.save(device);
    }

    @Override
    public Optional<UserDevice> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<UserDevice> findActiveByAccountId(Long accountId) {
        return repository.findActiveByAccountId(accountId);
    }

    @Override
    public List<UserDevice> findByAccountId(Long accountId) {
        return repository.findByAccountId(accountId);
    }

    @Override
    public Optional<UserDevice> findActiveByPushToken(String pushToken) {
        return repository.findActiveByPushToken(pushToken);
    }

    @Override
    public Optional<UserDevice> findActiveByPushEndpoint(String endpoint) {
        return repository.findActiveByPushEndpoint(endpoint);
    }

    @Override
    public List<UserDevice> findActiveByAccountIdAndPlatform(Long accountId, DevicePlatformCd platform) {
        return repository.findActiveByAccountIdAndPlatform(accountId, platform);
    }

    @Override
    public boolean existsByPushToken(String pushToken) {
        return repository.existsByPushTokenAndActiveTrue(pushToken);
    }

    @Override
    public boolean existsByPushEndpoint(String pushEndpoint) {
        return repository.existsByPushEndpointAndActiveTrue(pushEndpoint);
    }

    @Override
    public long countActiveByAccountId(Long accountId) {
        return repository.countActiveByAccountId(accountId);
    }

    @Override
    public void delete(UserDevice device) {
        repository.delete(device);
    }
}
