package com.ktb.interview.session.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "세션 응답용 토픽별 피드백")
public record InterviewSessionTopicFeedbackResponse(
        @JsonProperty("topic_id")
        @Schema(description = "토픽 ID", example = "1")
        Integer topicId,

        @JsonProperty("main_question")
        @Schema(description = "토픽 대표 질문")
        String mainQuestion,

        @JsonProperty("strengths")
        @Schema(description = "토픽 강점 피드백")
        String strengths,

        @JsonProperty("improvements")
        @Schema(description = "토픽 개선 피드백")
        String improvements
) {
}
