package com.ktb.ai.feedback.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "AI 피드백 응답")
public record AiFeedbackResponse(

        @JsonProperty("user_id")
        @Schema(description = "사용자 ID", example = "101")
        Long userId,

        @JsonProperty("question_id")
        @Schema(description = "질문 ID", example = "505")
        Long questionId,

        @JsonProperty("interview_type")
        @Schema(description = "인터뷰 타입", example = "PRACTICE_INTERVIEW",
                allowableValues = {"PRACTICE_INTERVIEW", "REAL_INTERVIEW"})
        String interviewType,

        @JsonProperty("question_type")
        @Schema(description = "질문 타입", example = "CS",
                allowableValues = {"CS", "SYSTEM_DESIGN", "PORTFOLIO"})
        String questionType,

        @JsonProperty("category")
        @Schema(description = "질문 카테고리", example = "DB",
                allowableValues = {
                        "OS", "NETWORK", "DB", "COMPUTER_ARCHITECTURE", "DATA_STRUCTURE_ALGORITHM",
                        "SOCIAL", "NOTIFICATION", "REALTIME", "SEARCH", "MEDIA", "STORAGE", "PLATFORM", "TRANSACTION",
                        "PORTFOLIO"
                })
        String category,

        @JsonProperty("metrics")
        @Schema(description = "평가 지표 목록 (정상 케이스만)")
        List<AiFeedbackMetric> metrics,

        @JsonProperty("bad_case_feedback")
        @Schema(description = "Bad case 피드백 (bad case만)")
        AiFeedbackBadCaseFeedback badCaseFeedback,

        @JsonProperty("weakness")
        @Schema(description = "약점 존재 여부 (정상 케이스만)", example = "true")
        Boolean weakness,

        @JsonProperty("feedback")
        // bad case 응답에서는 metrics/weakness/feedback이 null입니다.
        @Schema(description = "종합 피드백 (정상 케이스만)")
        AiFeedbackFeedback feedback
) {
}
