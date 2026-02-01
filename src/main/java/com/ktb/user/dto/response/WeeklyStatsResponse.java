package com.ktb.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "주간 학습 통계")
public record WeeklyStatsResponse(
        @JsonProperty("week_summary")
        @Schema(description = "주간 요약 문구", example = "이번주 연습 12회", requiredMode = Schema.RequiredMode.REQUIRED)
        String weekSummary,

        @JsonProperty("daily_stats")
        @Schema(description = "일별 학습 통계 (월~일)", requiredMode = Schema.RequiredMode.REQUIRED)
        List<DailyStatEntry> dailyStats,

        @JsonProperty("max_value_for_chart")
        @Schema(description = "차트 Y축 최댓값 (최소 3)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        long maxValueForChart,

        @JsonProperty("total_this_week")
        @Schema(description = "이번 주 총 학습 횟수", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        long totalThisWeek
) {

    @Schema(description = "일별 학습 통계 항목")
    public record DailyStatEntry(
            @Schema(description = "날짜", example = "2025-01-20", requiredMode = Schema.RequiredMode.REQUIRED)
            LocalDate date,

            @JsonProperty("day_of_week")
            @Schema(description = "요일 (한글)", example = "월", requiredMode = Schema.RequiredMode.REQUIRED)
            String dayOfWeek,

            @JsonProperty("real_count")
            @Schema(description = "해당 일 학습 횟수", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
            long realCount
    ) {
    }
}
