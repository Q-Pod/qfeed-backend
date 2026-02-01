package com.ktb.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카테고리별 학습 분포")
public record CategoryDistributionResponse(
        @JsonProperty("category")
        @Schema(description = "카테고리명", example = "OS", requiredMode = Schema.RequiredMode.REQUIRED)
        String category,

        @JsonProperty("count")
        @Schema(description = "카테고리별 학습 횟수", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
        long count
) {
}
