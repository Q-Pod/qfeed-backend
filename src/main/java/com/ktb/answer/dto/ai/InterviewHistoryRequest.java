package com.ktb.answer.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ktb.question.domain.QuestionCategory;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InterviewHistoryRequest(
        @JsonProperty("question")
        String question,

        @JsonProperty("answer_text")
        String answerText,

        @JsonProperty("turn_type")
        String turnType,

        @JsonProperty("turn_order")
        int turnOrder,

        @JsonProperty("topic_id")
        Integer topicId,

        @JsonProperty("category")
        QuestionCategory category
) {
    public InterviewHistoryRequest(
            String question,
            String answerText,
            String turnType,
            int turnOrder,
            Integer topicId
    ) {
        this(question, answerText, turnType, turnOrder, topicId, null);
    }
}
