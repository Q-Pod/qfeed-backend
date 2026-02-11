package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class NotificationTargetNotFoundException extends BusinessException {

    public NotificationTargetNotFoundException() {
        super(ErrorCode.NOTIFICATION_TARGET_NOT_FOUND);
    }

    public NotificationTargetNotFoundException(Long targetId) {
        super(ErrorCode.NOTIFICATION_TARGET_NOT_FOUND,
            String.format("발송 대상 ID: %d", targetId));
    }
}
