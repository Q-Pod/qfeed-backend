package com.ktb.answer.dto.response.detail;

import com.ktb.answer.dto.response.common.MetricScore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "AI 피드백 상세")
public record AiFeedbackDetail(
    @Schema(description = "피드백 상태", example = "COMPLETED",
        allowableValues = {"PROCESSING", "COMPLETED", "FAILED"})
    String status,

    @Schema(description = "AI 종합 평가 피드백",
        example = "전반적으로 프로세스와 스레드의 개념을 잘 이해하고 있습니다...")
    String feedback,

    @Schema(description = "평가 지표 점수 목록 (레이더 차트)")
    List<MetricScore> metrics
) {
}
