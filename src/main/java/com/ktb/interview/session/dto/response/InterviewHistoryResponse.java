package com.ktb.interview.session.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "세션 내 단일 인터뷰 turn 이력")
public record InterviewHistoryResponse(
        @Schema(description = "질문 본문")
        String question,

        @Schema(description = "질문 카테고리", nullable = true, example = "OS")
        String category,

        @JsonProperty("answer_text")
        @Schema(description = "답변 본문")
        String answerText,

        @JsonProperty("turn_type")
        @Schema(description = "turn 유형(new_topic/follow_up)")
        String turnType,

        @JsonProperty("turn_order")
        @Schema(description = "turn 순서", example = "0")
        int turnOrder,

        @JsonProperty("topic_id")
        @Schema(description = "topic ID", example = "1")
        Integer topicId
) {
}
