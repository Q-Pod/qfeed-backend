package com.ktb.answer.dto.response.list;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "답변 목록 조회 응답")
public record AnswerListResponse(

        @Schema(description = "답변 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        List<AnswerSummary> records,

        @Schema(description = "페이지네이션 정보", requiredMode = Schema.RequiredMode.REQUIRED)
        PaginationInfo pagination
) {
}
