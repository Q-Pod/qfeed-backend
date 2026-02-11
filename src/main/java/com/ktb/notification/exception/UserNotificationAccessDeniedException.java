package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class UserNotificationAccessDeniedException extends BusinessException {

    public UserNotificationAccessDeniedException() {
        super(ErrorCode.USER_NOTIFICATION_ACCESS_DENIED);
    }
}
