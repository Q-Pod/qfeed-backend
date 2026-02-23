package com.ktb.interview.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewBadCaseFeedbackResponse(
        @JsonProperty("type")
        String type,

        @JsonProperty("message")
        String message,

        @JsonProperty("guidance")
        String guidance
) {
}
