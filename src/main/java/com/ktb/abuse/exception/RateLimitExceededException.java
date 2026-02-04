package com.ktb.abuse.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class RateLimitExceededException extends BusinessException {

    public RateLimitExceededException() {
        super(ErrorCode.ABUSE_RATE_LIMIT_EXCEEDED);
    }

    public RateLimitExceededException(String detail) {
        super(ErrorCode.ABUSE_RATE_LIMIT_EXCEEDED, detail);
    }
}
