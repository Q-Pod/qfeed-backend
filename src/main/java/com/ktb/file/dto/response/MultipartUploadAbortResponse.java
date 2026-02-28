package com.ktb.file.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ktb.file.domain.FileUploadStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Multipart 업로드 중단 응답")
public record MultipartUploadAbortResponse(

        @JsonProperty("file_id")
        @Schema(description = "파일 ID", example = "456", requiredMode = Schema.RequiredMode.REQUIRED)
        Long fileId,

        @Schema(description = "업로드 상태", example = "FAILED", requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"PENDING", "UPLOADED", "FAILED"})
        FileUploadStatus status
) {
}
