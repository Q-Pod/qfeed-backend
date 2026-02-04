package com.ktb.answer.dto;

import com.ktb.answer.domain.AnswerStatus;

public enum FeedbackStatus {
    PROCESSING,
    COMPLETED,
    FAILED,
    FAILED_RETRYABLE,
    NOT_AVAILABLE;

    /**
     * AnswerStatus를 FeedbackStatus로 변환
     */
    public static FeedbackStatus from(AnswerStatus answerStatus) {
        return switch (answerStatus) {
            case AI_FEEDBACK_PROCESSING -> PROCESSING;
            case COMPLETED -> COMPLETED;
            case FAILED -> FAILED;
            case FAILED_RETRYABLE -> FAILED_RETRYABLE;
            default -> NOT_AVAILABLE;
        };
    }
}
