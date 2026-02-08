package com.ktb.answer.dto.response.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "키워드 체크 결과")
public record KeywordCheck(
    @Schema(description = "키워드", example = "프로세스")
    String keyword,

    @Schema(description = "답변에 포함 여부", example = "true")
    boolean included
) {
}
