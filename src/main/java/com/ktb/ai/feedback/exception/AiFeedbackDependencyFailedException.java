package com.ktb.ai.feedback.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

/**
 * AI 의존 서비스 장애로 피드백 처리를 완료할 수 없는 경우의 예외.
 */
public class AiFeedbackDependencyFailedException extends BusinessException {

    public AiFeedbackDependencyFailedException() {
        super(ErrorCode.AI_FEEDBACK_DEPENDENCY_FAILED);
    }

    public AiFeedbackDependencyFailedException(String detailMessage) {
        super(ErrorCode.AI_FEEDBACK_DEPENDENCY_FAILED, detailMessage);
    }

    public AiFeedbackDependencyFailedException(String detailMessage, Throwable cause) {
        super(ErrorCode.AI_FEEDBACK_DEPENDENCY_FAILED, detailMessage, cause);
    }
}
