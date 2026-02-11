package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class NotificationTargetDuplicateException extends BusinessException {

    public NotificationTargetDuplicateException() {
        super(ErrorCode.NOTIFICATION_TARGET_DUPLICATE);
    }

    public NotificationTargetDuplicateException(String dedupeKey) {
        super(ErrorCode.NOTIFICATION_TARGET_DUPLICATE,
            String.format("중복 키: %s", dedupeKey));
    }
}
