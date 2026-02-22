package com.ktb.ai.tts.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "TTS 변환 요청")
@JsonIgnoreProperties(ignoreUnknown = true)
public record TtsRequest(

        @JsonProperty("user_id")
        @Schema(description = "사용자 ID", example = "102", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "사용자 ID는 필수입니다")
        Long userId,

        @JsonProperty("session_id")
        @Schema(description = "면접 세션 식별자", example = "3333", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "세션 ID는 필수입니다")
        String sessionId,

        @JsonProperty("text")
        @Schema(
                description = "음성 변환 텍스트",
                example = "개념에 대해 정확하게 알고 계시네요. 그렇다면 4-way Handshake는 언제 발생하나요?",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "변환할 텍스트는 필수입니다")
        String text
) {
}

