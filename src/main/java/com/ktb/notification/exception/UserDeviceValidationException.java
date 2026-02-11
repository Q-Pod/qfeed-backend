package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class UserDeviceValidationException extends BusinessException {

    public UserDeviceValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UserDeviceValidationException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
