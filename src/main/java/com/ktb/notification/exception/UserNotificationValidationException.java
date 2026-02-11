package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class UserNotificationValidationException extends BusinessException {

    public UserNotificationValidationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
