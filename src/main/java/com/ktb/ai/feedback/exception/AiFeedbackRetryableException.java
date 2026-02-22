package com.ktb.ai.feedback.exception;

/**
 * 재시도로 복구 가능성이 있는 AI 호출 오류.
 */
public class AiFeedbackRetryableException extends RuntimeException {

    public AiFeedbackRetryableException(String message) {
        super(message);
    }

    public AiFeedbackRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
