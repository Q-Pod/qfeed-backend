package com.ktb.abuse.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class LowQualityContentException extends BusinessException {

    public LowQualityContentException() {
        super(ErrorCode.ABUSE_LOW_QUALITY_CONTENT);
    }

    public LowQualityContentException(String detail) {
        super(ErrorCode.ABUSE_LOW_QUALITY_CONTENT, detail);
    }
}
