package com.ktb.answer.dto.response.list;

import com.ktb.answer.dto.response.detail.AnswerQuestionInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "답변 요약 정보")
public record AnswerSummary(
    @Schema(description = "답변 ID", example = "1")
    Long answerId,

    @Schema(description = "답변 타입", example = "PRACTICE_INTERVIEW")
    String type,

    @Schema(description = "답변 작성 시각", example = "2026-01-22T10:30:00")
    String createdAt,

    @Schema(description = "질문 정보")
    AnswerQuestionInfo question,

    @Schema(description = "피드백 요약 정보")
    FeedbackInfo feedback
) {
}
