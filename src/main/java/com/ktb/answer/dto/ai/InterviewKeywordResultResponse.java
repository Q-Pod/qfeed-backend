package com.ktb.answer.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewKeywordResultResponse(
        @JsonProperty("covered_keywords")
        List<String> coveredKeywords,

        @JsonProperty("missing_keywords")
        List<String> missingKeywords,

        @JsonProperty("coverage_ratio")
        Double coverageRatio
) {
}
