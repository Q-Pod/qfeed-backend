package com.ktb.abuse.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class DuplicateContentException extends BusinessException {

    public DuplicateContentException() {
        super(ErrorCode.ABUSE_DUPLICATE_CONTENT);
    }

    public DuplicateContentException(String detail) {
        super(ErrorCode.ABUSE_DUPLICATE_CONTENT, detail);
    }
}
