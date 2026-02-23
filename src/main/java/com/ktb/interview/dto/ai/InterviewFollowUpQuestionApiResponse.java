package com.ktb.interview.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewFollowUpQuestionApiResponse(
        String message,
        InterviewFollowUpQuestionDataResponse data
) {
}
