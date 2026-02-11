package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class NoticeNotFoundException extends BusinessException {

    public NoticeNotFoundException() {
        super(ErrorCode.NOTICE_NOT_FOUND);
    }

    public NoticeNotFoundException(Long noticeId) {
        super(ErrorCode.NOTICE_NOT_FOUND,
            String.format(
                "%s, ID: %d",
                ErrorCode.NOTICE_NOT_FOUND.getMessage(),
                noticeId
            ));
    }
}
