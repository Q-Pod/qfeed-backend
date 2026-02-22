package com.ktb.answer.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewFeedbackMetricResponse(
        String name,
        Integer score,
        String comment
) {
}
