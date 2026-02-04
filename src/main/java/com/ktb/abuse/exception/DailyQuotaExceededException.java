package com.ktb.abuse.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class DailyQuotaExceededException extends BusinessException {

    public DailyQuotaExceededException() {
        super(ErrorCode.ABUSE_DAILY_QUOTA_EXCEEDED);
    }

    public DailyQuotaExceededException(String detail) {
        super(ErrorCode.ABUSE_DAILY_QUOTA_EXCEEDED, detail);
    }
}
