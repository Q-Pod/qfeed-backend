package com.ktb.answer.dto;

public record QuestionSummary(
        Long questionId,
        String content,
        String category,
        String type
) {
}
