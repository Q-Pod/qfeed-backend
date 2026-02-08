package com.ktb.answer.dto.response.list;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "피드백 요약 정보")
public record FeedbackInfo(
    @Schema(description = "피드백 제공 여부", example = "true")
    boolean feedbackAvailable,

    @Schema(description = "AI 피드백 상태", example = "COMPLETED",
        allowableValues = {"AI_FEEDBACK_PROCESSING", "COMPLETED", "FAILED", "FAILED_RETRYABLE", "NOT_AVAILABLE"})
    String aiFeedbackStatus
) {
}
