package com.ktb.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "학습 통계 요약")
public record LearningStatsResponse(
        @JsonProperty("practice_mode_count")
        @Schema(description = "연습 모드 학습 횟수", example = "45", requiredMode = Schema.RequiredMode.REQUIRED)
        long practiceModeCount,

        @JsonProperty("real_interview_count")
        @Schema(description = "실전 모드 학습 횟수", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
        long realInterviewCount,

        @JsonProperty("category_distribution")
        @Schema(description = "카테고리별 학습 분포", requiredMode = Schema.RequiredMode.REQUIRED)
        List<CategoryDistributionResponse> categoryDistribution,

        @JsonProperty("distinct_days")
        @Schema(description = "총 학습 일수", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
        int streakDays,

        @JsonProperty("total_questions_answered")
        @Schema(description = "총 답변 수", example = "53", requiredMode = Schema.RequiredMode.REQUIRED)
        long totalQuestionsAnswered
) {
}
