package com.ktb.answer.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InterviewFeedbackDataResponse(
        @JsonProperty("answer_id")
        Long answerId,

        @JsonProperty("user_id")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long userId,

        @JsonProperty("question_id")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long questionId,

        @JsonProperty("session_id")
        String sessionId,

        @JsonProperty("status")
        String status,

        @JsonProperty("bad_case_feedback")
        InterviewBadCaseFeedbackResponse badCaseFeedback,

        @JsonProperty("metrics")
        List<InterviewFeedbackMetricResponse> metrics,

        @JsonProperty("keyword_result")
        InterviewKeywordResultResponse keywordResult,

        @JsonProperty("topics_feedback")
        List<InterviewTopicFeedbackResponse> topicsFeedback,

        @JsonProperty("overall_feedback")
        InterviewOverallFeedbackResponse overallFeedback,

        @JsonProperty("next_question")
        InterviewNextQuestionResponse nextQuestion,

        @JsonProperty("next_turn_type")
        String nextTurnType,

        @JsonProperty("next_topic_id")
        Integer nextTopicId,

        @JsonProperty("is_final")
        Boolean isFinal
) {

    public InterviewFeedbackDataResponse withAnswerId(Long answerId) {
        return new InterviewFeedbackDataResponse(
                answerId,
                userId,
                questionId,
                sessionId,
                status,
                badCaseFeedback,
                metrics,
                keywordResult,
                topicsFeedback,
                overallFeedback,
                nextQuestion,
                nextTurnType,
                nextTopicId,
                isFinal
        );
    }

    public InterviewFeedbackDataResponse withStatus(String status) {
        return new InterviewFeedbackDataResponse(
                answerId,
                userId,
                questionId,
                sessionId,
                status,
                badCaseFeedback,
                metrics,
                keywordResult,
                topicsFeedback,
                overallFeedback,
                nextQuestion,
                nextTurnType,
                nextTopicId,
                isFinal
        );
    }

    public InterviewFeedbackDataResponse withoutIdentifiers() {
        return new InterviewFeedbackDataResponse(
                answerId,
                null,
                null,
                sessionId,
                status,
                badCaseFeedback,
                metrics,
                keywordResult,
                topicsFeedback,
                overallFeedback,
                nextQuestion,
                nextTurnType,
                nextTopicId,
                isFinal
        );
    }
}
