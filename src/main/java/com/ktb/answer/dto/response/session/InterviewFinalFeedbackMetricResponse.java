package com.ktb.answer.dto.response.session;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "최종 피드백 평가 지표")
public record InterviewFinalFeedbackMetricResponse(
        @Schema(description = "지표명", example = "정확도")
        String name,
        @Schema(description = "점수", example = "5")
        Integer score
) {
}
