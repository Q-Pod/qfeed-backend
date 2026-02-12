package com.ktb.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "질문 카테고리 목록 응답")
public record QuestionCategoryListResponse(
        @Schema(description = "질문 유형별 카테고리 맵 (key: questionType, value: 카테고리 맵)")
        Map<String, Map<String, String>> categories
) {
}
