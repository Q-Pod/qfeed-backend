package com.ktb.answer.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewFollowUpQuestionApiResponse(
        String message,
        InterviewFollowUpQuestionDataResponse data
) {
}
