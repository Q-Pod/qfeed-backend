package com.ktb.file.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Multipart 파트 Presigned URL 발급 응답")
public record MultipartPartPresignedUrlResponse(

        @JsonProperty("file_id")
        @Schema(description = "파일 ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
        Long fileId,

        @JsonProperty("part_number")
        @Schema(description = "파트 번호(1-base)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        int partNumber,

        @JsonProperty("presigned_url")
        @Schema(description = "해당 파트 업로드용 Presigned URL",
                example = "https://s3.amazonaws.com/bucket/video/uuid.webm?...",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String presignedUrl,

        @JsonProperty("expires_in")
        @Schema(description = "URL 만료 시간 (초)", example = "300", requiredMode = Schema.RequiredMode.REQUIRED)
        int expiresIn,

        @JsonProperty("method")
        @Schema(description = "HTTP 메서드", example = "PUT", requiredMode = Schema.RequiredMode.REQUIRED)
        String method
) {
}
