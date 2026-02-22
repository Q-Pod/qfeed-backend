package com.ktb.answer.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewFollowUpQuestionDataResponse(
        @JsonProperty("user_id")
        Long userId,

        @JsonProperty("session_id")
        String sessionId,

        @JsonProperty("question_text")
        String questionText,

        @JsonProperty("category")
        String category,

        @JsonProperty("topic_id")
        Integer topicId,

        @JsonProperty("turn_type")
        String turnType,

        @JsonProperty("is_session_ended")
        Boolean isSessionEnded,

        @JsonProperty("end_reason")
        String endReason,

        @JsonProperty("is_bad_case")
        Boolean isBadCase,

        @JsonProperty("bad_case_feedback")
        InterviewBadCaseFeedbackResponse badCaseFeedback
) {
}
