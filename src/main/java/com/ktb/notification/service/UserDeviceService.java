package com.ktb.notification.service;

import com.ktb.notification.dto.request.DeviceRegisterRequest;
import com.ktb.notification.dto.response.UserDeviceResponse;
import java.util.List;

public interface UserDeviceService {

    UserDeviceResponse registerDevice(Long accountId, DeviceRegisterRequest request);

    List<UserDeviceResponse> getMyDevices(Long accountId);

    void deactivateDevice(Long accountId, Long deviceId);
}
