package com.ktb.ai.feedback.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "AI 피드백 생성 요청")
public record AiFeedbackRequest(

        @JsonProperty("user_id")
        @Schema(description = "사용자 ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "사용자 ID는 필수입니다")
        Long userId,

        @JsonProperty("question_id")
        @Schema(description = "질문 ID", example = "505", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "질문 ID는 필수입니다")
        Long questionId,

        @JsonProperty("question_type")
        @Schema(description = "질문 타입", example = "PRACTICE_INTERVIEW", requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"CS", "SYSTEM_DESIGN", "PORTFOLIO"})
        @NotBlank(message = "질문 타입은 필수입니다")
        String type,

        @JsonProperty("category")
        // 현재 QuestionCategory enum 기준 전체 카테고리 목록입니다.
        @Schema(description = "질문 카테고리", example = "DB", requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {
                        "OS", "NETWORK", "DB", "COMPUTER_ARCHITECTURE", "DATA_STRUCTURE_ALGORITHM",
                        "SOCIAL", "NOTIFICATION", "REALTIME", "SEARCH", "MEDIA", "STORAGE", "PLATFORM", "TRANSACTION",
                        "PORTFOLIO"
                })
        String category,

        @JsonProperty("interview_type")
        @Schema(description = "인터뷰 카테고리", example = "PRACTICE_INTERVIEW", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"PRACTICE_INTERVIEW", "REAL_INTERVIEW"})
        @NotBlank(message = "인터뷰 카테고리는 필수입니다")
        String interviewType,

        @JsonProperty("question")
        @Schema(description = "질문 내용", example = "RDBMS와 NoSQL의 차이점에 대해 설명해주세요.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "질문 내용은 필수입니다")
        String question,

        @JsonProperty("answer_text")
        @Schema(description = "답변 내용", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "답변 내용은 필수입니다")
        String answerText
) {
}
