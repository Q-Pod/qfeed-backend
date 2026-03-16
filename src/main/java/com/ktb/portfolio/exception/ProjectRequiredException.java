package com.ktb.portfolio.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class ProjectRequiredException extends BusinessException {
    public ProjectRequiredException() {
        super(ErrorCode.PROJECT_REQUIRED);
    }
}
