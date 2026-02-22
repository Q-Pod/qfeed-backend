package com.ktb.ai.feedback.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

/**
 * AI 서버가 요청 자체를 거부한 경우의 예외(주로 4xx).
 */
public class AiFeedbackRequestRejectedException extends BusinessException {

    public AiFeedbackRequestRejectedException(String message) {
        super(ErrorCode.INVALID_INPUT, message);
    }
}
