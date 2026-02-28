package com.ktb.file.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "Multipart 업로드 완료 요청")
public record MultipartUploadCompleteRequest(

        @JsonProperty("parts")
        @NotEmpty(message = "parts는 1개 이상이어야 합니다")
        @Schema(description = "업로드된 파트 목록(정렬되지 않아도 됩니다)", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@Valid MultipartUploadedPartRequest> parts
) {
}
