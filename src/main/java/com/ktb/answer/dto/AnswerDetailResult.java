package com.ktb.answer.dto;

import com.ktb.answer.domain.AnswerStatus;
import com.ktb.answer.domain.AnswerType;

public record AnswerDetailResult(
        Long answerId,
        AnswerStatus status,
        AnswerType type,
        QuestionSummary question,
        AnswerContentResult answer,
        ImmediateFeedbackResult immediateFeedback,
        AiFeedbackSummary aiFeedback
) {
}
