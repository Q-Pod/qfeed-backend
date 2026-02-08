package com.ktb.answer.dto.response.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "평가 지표 점수")
public record MetricScore(
    @Schema(description = "평가 지표명", example = "논리 구조")
    String metricName,

    @Schema(description = "평가 지표 설명", example = "답변의 논리적 흐름과 구조")
    String metricDescription,

    @Schema(description = "획득 점수", example = "4", minimum = "1", maximum = "5")
    int score,

    @Schema(description = "최대 점수", example = "5")
    int maxScore
) {
}
