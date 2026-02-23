package com.ktb.interview.dto.request;

import com.ktb.answer.domain.AnswerType;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "인터뷰 세션 생성 요청")
public record InterviewSessionCreateRequest(
        @NotNull(message = "interviewType is required")
        @Schema(
                description = "면접 모드",
                allowableValues = {"PRACTICE_INTERVIEW", "REAL_INTERVIEW"},
                example = "REAL_INTERVIEW"
        )
        AnswerType interviewType,

        @NotNull(message = "questionType is required")
        @Schema(
                description = "질문 유형",
                allowableValues = {"CS", "SYSTEM_DESIGN", "PORTFOLIO"},
                example = "CS"
        )
        QuestionType questionType,

        @Schema(
                description = "질문 카테고리(실전 모드에서 AI 첫 질문 생성 시 사용)",
                nullable = true,
                example = "OS"
        )
        QuestionCategory category

) {
}
