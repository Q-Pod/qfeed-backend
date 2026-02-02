package com.ktb.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "질문 타입 목록 응답")
public record QuestionTypeListResponse(
        @Schema(description = "질문 타입 맵 (key: enum name, value: label)")
        Map<String, String> types
) {
}
