package com.ktb.interview.session.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "세션 응답용 bad case 피드백")
public record InterviewSessionBadCaseFeedbackResponse(
        @JsonProperty("type")
        @Schema(description = "bad case 타입", example = "INSUFFICIENT")
        String type,

        @JsonProperty("message")
        @Schema(description = "bad case 메시지")
        String message,

        @JsonProperty("guidance")
        @Schema(description = "개선 가이드")
        String guidance
) {
}
