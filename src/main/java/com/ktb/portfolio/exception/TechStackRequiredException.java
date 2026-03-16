package com.ktb.portfolio.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class TechStackRequiredException extends BusinessException {
    public TechStackRequiredException() {
        super(ErrorCode.TECH_STACK_REQUIRED);
    }
}
