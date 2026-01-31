package com.ktb.answer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 답변 목록 조회 응답 DTO
 */
@Schema(description = "답변 목록 조회 응답")
public record AnswerListResponse(

        @Schema(description = "답변 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        List<AnswerSummary> records,

        @Schema(description = "페이지네이션 정보", requiredMode = Schema.RequiredMode.REQUIRED)
        PaginationInfo pagination
) {

    @Schema(description = "답변 요약 정보")
    public record AnswerSummary(
            @Schema(description = "답변 ID", example = "1")
            Long answerId,

            @Schema(description = "답변 타입", example = "PRACTICE_INTERVIEW")
            String type,

            @Schema(description = "답변 작성 시각", example = "2026-01-22T10:30:00")
            String createdAt,

        @Schema(description = "질문 정보")
        QuestionInfo question,

        @Schema(description = "피드백 요약 정보")
        FeedbackInfo feedback
    ) {
    }

    @Schema(description = "질문 요약 정보")
    public record QuestionInfo(
            @Schema(description = "질문 ID", example = "10")
            Long questionId,

            @Schema(description = "질문 내용", example = "프로세스와 스레드의 차이를 설명해주세요")
            String content,

            @Schema(description = "질문 카테고리", example = "OS")
            String category
    ) {
    }

    @Schema(description = "피드백 요약 정보")
    public record FeedbackInfo(
            @Schema(description = "피드백 제공 여부", example = "true")
            boolean feedbackAvailable,

            @Schema(description = "AI 피드백 상태", example = "COMPLETED",
                    allowableValues = {"PROCESSING", "COMPLETED", "FAILED", "FAILED_RETRYABLE", "NOT_AVAILABLE"})
            String aiFeedbackStatus
    ) {
    }

    @Schema(description = "페이지네이션 정보")
    public record PaginationInfo(
            @Schema(description = "페이지 크기", example = "10")
            int limit,

            @Schema(description = "다음 페이지 존재 여부", example = "true")
            boolean hasNext,

            @Schema(description = "다음 페이지 커서 (hasNext=true 시에만 제공)",
                    example = "eyJsYXN0X2NyZWF0ZWRfYXQiOiIyMDI2LTAxLTIyVDEwOjAwOjAwIiwibGFzdF9hbnN3ZXJfaWQiOjEwfQ==")
            String nextCursor
    ) {
    }
}
