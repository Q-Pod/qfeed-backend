package com.ktb.answer.dto;

import com.ktb.answer.dto.response.AnswerSubmitResponse;

public record AnswerSubmitResult(
        Long answerId,
        ImmediateFeedbackResult immediateFeedback,
        FeedbackStatus aiFeedbackStatus
) {

    private static final FeedbackStatus DEFAULT_FEEDBACK_STATUS = FeedbackStatus.PROCESSING;

    public static AnswerSubmitResult processing(Long answerId, ImmediateFeedbackResult feedback) {
        return new AnswerSubmitResult(answerId, feedback, DEFAULT_FEEDBACK_STATUS);
    }

    public static AnswerSubmitResult noAiFeedback(Long answerId, ImmediateFeedbackResult feedback) {
        return new AnswerSubmitResult(answerId, feedback, FeedbackStatus.NOT_AVAILABLE);
    }

    public AnswerSubmitResponse from() {
        return new AnswerSubmitResponse(
            this.answerId,
            this.immediateFeedback.of(),
            this.aiFeedbackStatus.name()
        );
    }
}
