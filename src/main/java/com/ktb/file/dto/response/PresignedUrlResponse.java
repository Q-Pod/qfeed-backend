package com.ktb.file.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ktb.file.domain.UploadMode;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Presigned URL 생성 응답")
public record PresignedUrlResponse(

        @JsonProperty("file_id")
        @Schema(description = "생성된 파일 ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
        Long fileId,

        @JsonProperty("upload_mode")
        @Schema(description = "업로드 방식", example = "SINGLE", requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"SINGLE", "MULTIPART"})
        UploadMode uploadMode,

        @JsonProperty("presigned_url")
        @Schema(description = "S3 Presigned URL",
                example = "https://s3.amazonaws.com/bucket/uploads/audio/uuid.mp3?X-Amz-Algorithm=...",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String presignedUrl,

        @JsonProperty("part_size")
        @Schema(description = "multipart 업로드 조각 크기(bytes), MULTIPART일 때만 포함",
                example = "8388608",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer partSize,

        @JsonProperty("expires_in")
        @Schema(description = "URL 만료 시간 (초)", example = "300", requiredMode = Schema.RequiredMode.REQUIRED)
        int expiresIn,

        @JsonProperty("method")
        @Schema(description = "HTTP 메서드(SINGLE/GET 조회 시 포함)", example = "PUT",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String method
) {

    public static PresignedUrlResponse single(Long fileId, String presignedUrl, int expiresIn, String method) {
        return new PresignedUrlResponse(fileId, UploadMode.SINGLE, presignedUrl, null, expiresIn, method);
    }

    public static PresignedUrlResponse multipart(Long fileId, int expiresIn, int partSize) {
        return new PresignedUrlResponse(fileId, UploadMode.MULTIPART, null, partSize, expiresIn, null);
    }
}
