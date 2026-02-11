package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class UserDeviceAlreadyRegisteredException extends BusinessException {

    public UserDeviceAlreadyRegisteredException() {
        super(ErrorCode.DEVICE_ALREADY_REGISTERED);
    }

    public UserDeviceAlreadyRegisteredException(String token) {
        super(ErrorCode.DEVICE_ALREADY_REGISTERED,
            String.format("토큰: %s", token));
    }
}
