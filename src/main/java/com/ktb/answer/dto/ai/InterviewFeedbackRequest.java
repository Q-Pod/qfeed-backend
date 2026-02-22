package com.ktb.answer.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InterviewFeedbackRequest(
        @JsonProperty("user_id")
        Long userId,

        @JsonProperty("question_id")
        Long questionId,

        @JsonProperty("session_id")
        String sessionId,

        @JsonProperty("interview_type")
        String interviewType,

        @JsonProperty("question_type")
        String questionType,

        @JsonProperty("category")
        String category,

        @JsonProperty("interview_history")
        List<InterviewHistoryRequest> interviewHistory,

        @JsonProperty("keywords")
        List<String> keywords
) {
}
