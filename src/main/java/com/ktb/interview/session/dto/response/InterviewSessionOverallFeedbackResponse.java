package com.ktb.interview.session.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "세션 응답용 종합 피드백")
public record InterviewSessionOverallFeedbackResponse(
        @JsonProperty("strengths")
        @Schema(description = "강점 피드백")
        String strengths,

        @JsonProperty("improvements")
        @Schema(description = "개선점 피드백")
        String improvements
) {
}
