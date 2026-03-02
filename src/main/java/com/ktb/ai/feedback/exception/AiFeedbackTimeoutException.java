package com.ktb.ai.feedback.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

/**
 * AI 의존 서비스 호출 중 타임아웃이 발생한 경우의 예외.
 */
public class AiFeedbackTimeoutException extends BusinessException {

    public AiFeedbackTimeoutException(String detailMessage) {
        super(ErrorCode.AI_FEEDBACK_TIMEOUT, detailMessage);
    }

    public AiFeedbackTimeoutException(String detailMessage, Throwable cause) {
        super(ErrorCode.AI_FEEDBACK_TIMEOUT, detailMessage, cause);
    }
}
