package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;
import com.ktb.notification.domain.enums.NoticeStatusCd;

public class NoticeInvalidStatusTransitionException extends BusinessException {

    public NoticeInvalidStatusTransitionException() {
        super(ErrorCode.NOTICE_INVALID_STATUS_TRANSITION);
    }

    public NoticeInvalidStatusTransitionException(NoticeStatusCd from, NoticeStatusCd to) {
        super(ErrorCode.NOTICE_INVALID_STATUS_TRANSITION,
                String.format(
                    "%s, 상태 전이 불가: %s → %s",
                    ErrorCode.NOTICE_INVALID_STATUS_TRANSITION.getMessage(),
                    from.getDescription(),
                    to.getDescription()));
    }
}
