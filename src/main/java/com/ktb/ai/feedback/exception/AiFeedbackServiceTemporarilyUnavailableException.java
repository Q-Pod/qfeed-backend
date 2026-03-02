package com.ktb.ai.feedback.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

/**
 * AI 서비스가 일시적으로 유효한 응답을 제공하지 못한 경우의 예외.
 */
public class AiFeedbackServiceTemporarilyUnavailableException extends BusinessException {

    public AiFeedbackServiceTemporarilyUnavailableException(String detailMessage) {
        super(ErrorCode.AI_FEEDBACK_SERVICE_TEMPORARILY_UNAVAILABLE, detailMessage);
    }

    public AiFeedbackServiceTemporarilyUnavailableException(String detailMessage, Throwable cause) {
        super(ErrorCode.AI_FEEDBACK_SERVICE_TEMPORARILY_UNAVAILABLE, detailMessage, cause);
    }
}
