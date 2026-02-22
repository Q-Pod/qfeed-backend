package com.ktb.answer.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewNextQuestionResponse(
        @JsonProperty("question_id")
        Long questionId,

        @JsonProperty("content")
        String content,

        @JsonProperty("category")
        String category
) {
}
