package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class UserDeviceNotFoundException extends BusinessException {

    public UserDeviceNotFoundException() {
        super(ErrorCode.DEVICE_NOT_FOUND);
    }

    public UserDeviceNotFoundException(Long deviceId) {
        super(ErrorCode.DEVICE_NOT_FOUND,
            String.format("디바이스 ID: %d", deviceId));
    }
}
