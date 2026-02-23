package com.ktb.interview.dto.response.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "세션 피드백 실패 응답")
public record SessionFeedbackFailedResponse(
        @Schema(description = "세션 상태", example = "FAILED")
        String status,
        @Schema(description = "실패 메시지", example = "AI feedback dependency failed - status=502")
        String message,

        @JsonProperty("retry_count")
        @Schema(description = "재시도 횟수", example = "3")
        int retryCount,

        @JsonProperty("failed_at")
        @Schema(description = "최종 실패 시각(ISO-8601)", example = "2026-02-21T15:12:22")
        String failedAt,

        @JsonProperty("session_id")
        @Schema(description = "인터뷰 세션 ID", example = "2371d491-c5eb-4650-af9f-3fab24bdc72b")
        String sessionId,

        @JsonProperty("error_code")
        @Schema(description = "오류 코드", example = "AI013")
        String errorCode
) {
}
