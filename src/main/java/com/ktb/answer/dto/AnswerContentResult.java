package com.ktb.answer.dto;

public record AnswerContentResult(
        String answerText,
        String audioUrl,
        String videoUrl,
        String answeredAt
) {
}
