package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class UserNotificationNotFoundException extends BusinessException {

    public UserNotificationNotFoundException() {
        super(ErrorCode.USER_NOTIFICATION_NOT_FOUND);
    }

    public UserNotificationNotFoundException(Long notificationId) {
        super(ErrorCode.USER_NOTIFICATION_NOT_FOUND,
            String.format(
                "%s ID: %d",
                ErrorCode.USER_NOTIFICATION_NOT_FOUND.getMessage(),
                notificationId
            ));
    }
}
