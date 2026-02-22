package com.ktb.answer.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewFeedbackApiResponse(
        String message,
        InterviewFeedbackDataResponse data
) {
}
