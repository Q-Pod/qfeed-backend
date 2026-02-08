package com.ktb.answer.dto.response.submit;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "다음 질문 정보")
public record NextQuestion(
    @Schema(description = "다음 질문 ID", example = "15")
    Long questionId,

    @Schema(description = "다음 질문 내용", example = "동기화 기법에 대해 설명해주세요")
    String content,

    @Schema(description = "다음 질문 카테고리", example = "OS")
    String category
) {
}
