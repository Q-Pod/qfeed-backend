package com.ktb.techstack.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class TechStackNotFoundException extends BusinessException {

    public TechStackNotFoundException() {
        super(ErrorCode.TECH_STACK_NOT_FOUND);
    }

    public TechStackNotFoundException(Long techStackId) {
        super(ErrorCode.TECH_STACK_NOT_FOUND, "기술 스택을 찾을 수 없습니다: " + techStackId);
    }
}
