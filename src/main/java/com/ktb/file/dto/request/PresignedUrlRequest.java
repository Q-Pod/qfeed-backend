package com.ktb.file.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ktb.file.domain.FileCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

@Schema(description = "Presigned URL 생성 요청")
public record PresignedUrlRequest(

        @JsonProperty("file_name")
        @Schema(description = "파일명", example = "interview_answer.mp3", requiredMode = Schema.RequiredMode.REQUIRED)
        String fileName,

        @JsonProperty("file_size")
        @Schema(description = "파일 크기 (bytes)", example = "5242880", requiredMode = Schema.RequiredMode.REQUIRED)
        Long fileSize,

        @JsonProperty("mime_type")
        @Schema(description = "MIME 타입", example = "audio/mpeg", requiredMode = Schema.RequiredMode.REQUIRED)
        String mimeType,

        @JsonProperty("category")
        @Schema(description = "파일 카테고리", example = "AUDIO", requiredMode = Schema.RequiredMode.REQUIRED)
        FileCategory category,

        @JsonProperty("method")
        @Schema(description = "HTTP 메서드", example = "PUT",
                allowableValues = {"PUT", "GET"})
        PresignedUrlMethod method,

        @JsonProperty("file_id")
        @Schema(description = "기존 파일 ID (GET/HEAD용)", example = "123")
        Long fileId
) {

    @AssertTrue(message = "PUT 요청은 file_name, file_size, mime_type, category가 필요합니다")
    public boolean isValidPutRequest() {
        PresignedUrlMethod resolvedMethod = method == null ? PresignedUrlMethod.PUT : method;
        if (resolvedMethod != PresignedUrlMethod.PUT) {
            return true;
        }
        return fileName != null && !fileName.isBlank()
                && fileSize != null && fileSize >= 1
                && mimeType != null && !mimeType.isBlank()
                && category != null;
    }

    @AssertTrue(message = "GET 요청은 file_id가 필요합니다")
    public boolean isValidReadRequest() {
        PresignedUrlMethod resolvedMethod = method == null ? PresignedUrlMethod.PUT : method;
        if (resolvedMethod == PresignedUrlMethod.PUT) {
            return true;
        }
        return fileId != null;
    }
}
