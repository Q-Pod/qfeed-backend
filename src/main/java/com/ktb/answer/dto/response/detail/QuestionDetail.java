package com.ktb.answer.dto.response.detail;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "질문 상세 정보")
public record QuestionDetail(
    @Schema(description = "질문 ID", example = "10")
    Long questionId,

    @Schema(description = "질문 내용", example = "프로세스와 스레드의 차이를 설명해주세요")
    String content,

    @Schema(description = "질문 카테고리", example = "OS")
    String category,

    @Schema(description = "질문 타입", example = "TECHNICAL")
    String type,

    @Schema(description = "질문 핵심 키워드", example="")
    List<String> keywords
) {
}
