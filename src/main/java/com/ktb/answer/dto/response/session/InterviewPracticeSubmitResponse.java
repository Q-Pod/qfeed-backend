package com.ktb.answer.dto.response.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "연습 모드 답변 제출 응답")
public record InterviewPracticeSubmitResponse(
        @JsonProperty("user_id")
        @Schema(description = "사용자 ID", example = "102")
        Long userId,

        @JsonProperty("question_id")
        @Schema(description = "질문 ID", example = "1001")
        Long questionId,

        @JsonProperty("session_id")
        @Schema(description = "인터뷰 세션 ID", example = "2371d491-c5eb-4650-af9f-3fab24bdc72b")
        String sessionId,

        @Schema(description = "세션 처리 상태", example = "IN_PROGRESS")
        String status,

        @JsonProperty("turn_type")
        @Schema(description = "현재 turn 유형", example = "new_topic")
        String turnType,

        @JsonProperty("turn_order")
        @Schema(description = "현재 turn 순서", example = "0")
        Integer turnOrder,

        @JsonProperty("topic_id")
        @Schema(description = "현재 topic ID", example = "1")
        Integer topicId,

        @JsonProperty("is_final")
        @Schema(description = "최종 응답 여부", example = "false")
        Boolean isFinal
) {
}
