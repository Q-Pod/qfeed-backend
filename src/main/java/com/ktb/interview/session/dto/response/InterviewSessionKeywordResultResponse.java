package com.ktb.interview.session.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "세션 응답용 키워드 분석 결과")
public record InterviewSessionKeywordResultResponse(
        @JsonProperty("covered_keywords")
        @Schema(description = "포함된 키워드")
        List<String> coveredKeywords,

        @JsonProperty("missing_keywords")
        @Schema(description = "누락된 키워드")
        List<String> missingKeywords,

        @JsonProperty("coverage_ratio")
        @Schema(description = "키워드 커버리지 비율", example = "0.4")
        Double coverageRatio
) {
}
