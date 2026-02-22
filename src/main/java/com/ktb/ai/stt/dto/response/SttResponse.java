package com.ktb.ai.stt.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "STT 변환 응답")
public record SttResponse(

        @JsonProperty("user_id")
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @JsonProperty("session_id")
        @Schema(description = "면접 세션 식별자(UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @JsonProperty("text")
        @Schema(description = "변환된 텍스트",
                example = "RDBMS는 엄격한 스키마가 존재하고 SQL을 사용해 데이터를 관리하는 반면...")
        String text
) {
}
