package com.ktb.answer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "세션 최종 피드백 요청")
public record SessionFinalFeedbackRequest(
        @NotBlank(message = "sessionId is required")
        @Schema(description = "인터뷰 세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId
) {
}
