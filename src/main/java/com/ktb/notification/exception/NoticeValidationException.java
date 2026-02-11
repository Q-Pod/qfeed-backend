package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class NoticeValidationException extends BusinessException {

    public NoticeValidationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
