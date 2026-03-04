package com.ktb.notification.service.impl;

import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.exception.account.AccountNotFoundException;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.notification.domain.UserDevice;
import com.ktb.notification.dto.request.DeviceRegisterRequest;
import com.ktb.notification.dto.response.UserDeviceResponse;
import com.ktb.notification.exception.UserDeviceNotFoundException;
import com.ktb.notification.repository.UserDeviceRepository;
import com.ktb.notification.service.UserDeviceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDeviceServiceImpl implements UserDeviceService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional
    public UserDeviceResponse registerDevice(Long accountId, DeviceRegisterRequest request) {
        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        UserDevice device = UserDevice.create(account, request.platform());
        UserDevice saved = userDeviceRepository.save(device);

        return UserDeviceResponse.from(saved);
    }

    @Override
    public List<UserDeviceResponse> getMyDevices(Long accountId) {
        return userDeviceRepository.findByAccountId(accountId).stream()
                .map(UserDeviceResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void deactivateDevice(Long accountId, Long deviceId) {
        UserDevice device = userDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new UserDeviceNotFoundException(deviceId));

        if (!device.getAccount().getId().equals(accountId)) {
            throw new UserDeviceNotFoundException(deviceId);
        }

        device.deactivate();
    }
}
