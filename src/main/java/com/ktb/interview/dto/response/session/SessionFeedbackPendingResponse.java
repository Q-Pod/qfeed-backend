package com.ktb.interview.dto.response.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "세션 피드백 처리중 응답")
public record SessionFeedbackPendingResponse(
        @Schema(description = "세션 상태", example = "RETRYING")
        String status,

        @JsonProperty("retry_count")
        @Schema(description = "재시도 횟수", example = "1")
        int retryCount,

        @JsonProperty("next_retry_at")
        @Schema(description = "다음 재시도 예정 시각(ISO-8601)", nullable = true, example = "2026-02-21T15:12:23")
        String nextRetryAt
) {
}
