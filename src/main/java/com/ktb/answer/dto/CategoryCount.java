package com.ktb.answer.dto;

import com.ktb.question.domain.QuestionCategory;

public record CategoryCount(
        QuestionCategory category,
        long count
) {
}
