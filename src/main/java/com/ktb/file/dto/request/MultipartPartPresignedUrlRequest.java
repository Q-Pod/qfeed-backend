package com.ktb.file.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Multipart 파트 Presigned URL 발급 요청")
public record MultipartPartPresignedUrlRequest(

        @JsonProperty("part_number")
        @NotNull(message = "part_number는 필수입니다")
        @Min(value = 1, message = "part_number는 1 이상이어야 합니다")
        @Schema(description = "파트 번호(1-base)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer partNumber
) {
}
