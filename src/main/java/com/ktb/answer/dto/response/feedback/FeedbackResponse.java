package com.ktb.answer.dto.response.feedback;

import com.ktb.answer.dto.response.common.MetricScore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * AI 피드백 조회 응답 DTO
 */
@Schema(description = "AI 피드백 조회 응답")
public record FeedbackResponse(

        @Schema(description = "피드백 처리 상태", example = "COMPLETED",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"PROCESSING", "COMPLETED", "FAILED"})
        String status,

        @Schema(description = "AI 종합 평가 피드백 (status=COMPLETED 시)",
                example = "전반적으로 프로세스와 스레드의 개념을 잘 이해하고 계십니다. 특히 메모리 공간의 차이에 대해 명확히 설명하셨습니다...")
        String feedback,

        @Schema(description = "레이더 차트 데이터 (status=COMPLETED 시)")
        List<MetricScore> radarChart,

        @Schema(description = "재시도 권장 시간 (초, status=PROCESSING 시)", example = "5")
        Integer retryAfter
) {
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final int DEFAULT_RETRY_AFTER_SECONDS = 5;

    public static FeedbackResponse processing() {
        return new FeedbackResponse(
                STATUS_PROCESSING,
                null,
                null,
                DEFAULT_RETRY_AFTER_SECONDS
        );
    }

    public static FeedbackResponse completed(String feedback, List<MetricScore> radarChart) {
        return new FeedbackResponse(
                STATUS_COMPLETED,
                feedback,
                radarChart,
                null
        );
    }

    public static FeedbackResponse failed(String reason) {
        return new FeedbackResponse(
                STATUS_COMPLETED,
                reason,
                null,
                null
        );
    }
}
