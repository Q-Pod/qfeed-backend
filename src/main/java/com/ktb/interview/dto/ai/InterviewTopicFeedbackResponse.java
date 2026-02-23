package com.ktb.interview.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewTopicFeedbackResponse(
        @JsonProperty("topic_id")
        Integer topicId,

        @JsonProperty("main_question")
        String mainQuestion,

        @JsonProperty("strengths")
        String strengths,

        @JsonProperty("improvements")
        String improvements
) {
}
