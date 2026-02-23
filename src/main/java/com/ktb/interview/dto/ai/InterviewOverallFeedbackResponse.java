package com.ktb.interview.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewOverallFeedbackResponse(
        @JsonProperty("strengths")
        String strengths,

        @JsonProperty("improvements")
        String improvements
) {
}
