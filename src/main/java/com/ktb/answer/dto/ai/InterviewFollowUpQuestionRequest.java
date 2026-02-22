package com.ktb.answer.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ktb.question.domain.QuestionCategory;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InterviewFollowUpQuestionRequest(
        @JsonProperty("user_id")
        Long userId,

        @JsonProperty("session_id")
        String sessionId,

        @JsonProperty("question_type")
        String questionType,

        @JsonProperty("initial_category")
        QuestionCategory initialCategory,

        @JsonProperty("interview_history")
        List<InterviewHistoryRequest> interviewHistory
) {
    public InterviewFollowUpQuestionRequest {
        questionType = normalize(questionType);
    }

    public InterviewFollowUpQuestionRequest(
            Long userId,
            String sessionId,
            String questionType,
            List<InterviewHistoryRequest> interviewHistory
    ) {
        this(userId, sessionId, questionType, null, interviewHistory);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
