package com.ktb.answer.dto.response.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ktb.answer.dto.ai.InterviewBadCaseFeedbackResponse;
import com.ktb.answer.dto.ai.InterviewKeywordResultResponse;
import com.ktb.answer.dto.ai.InterviewOverallFeedbackResponse;
import com.ktb.answer.dto.ai.InterviewTopicFeedbackResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "세션 최종 피드백 응답")
public record InterviewSessionFinalFeedbackResponse(
        @JsonProperty("answer_id")
        @Schema(description = "답변 ID", example = "10038")
        Long answerId,

        @JsonProperty("user_id")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "사용자 ID", example = "102")
        Long userId,

        @JsonProperty("question_id")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "질문 ID", example = "1001")
        Long questionId,

        @JsonProperty("session_id")
        @Schema(description = "세션 ID", example = "4d348acf-b9ee-4a50-b8f5-02e52197165e")
        String sessionId,

        @Schema(description = "상태", example = "COMPLETED")
        String status,

        @JsonProperty("bad_case_feedback")
        @Schema(description = "Bad case 피드백")
        InterviewBadCaseFeedbackResponse badCaseFeedback,

        @Schema(description = "평가 지표 목록")
        List<InterviewFinalFeedbackMetricResponse> metrics,

        @JsonProperty("keyword_result")
        @Schema(description = "키워드 결과")
        InterviewKeywordResultResponse keywordResult,

        @JsonProperty("topics_feedback")
        @Schema(description = "토픽별 피드백")
        List<InterviewTopicFeedbackResponse> topicsFeedback,

        @JsonProperty("overall_feedback")
        @Schema(description = "종합 피드백")
        InterviewOverallFeedbackResponse overallFeedback
) {
}
