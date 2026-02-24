package com.ktb.interview.session.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "실전 모드 다음 질문 정보")
public record InterviewNextQuestionTurnResponse(
        @JsonProperty("question_id")
        @Schema(description = "질문 ID(생성 질문이면 null)", nullable = true)
        Long questionId,

        @Schema(description = "질문 본문", example = "뮤텍스와 세마포어의 차이를 설명해주세요.")
        String content,

        @Schema(description = "질문 카테고리", nullable = true, example = "OS")
        String category,

        @JsonProperty("turn_type")
        @Schema(description = "다음 turn 유형(follow_up/new_topic/session_end)", example = "follow_up")
        String turnType,

        @JsonProperty("topic_id")
        @Schema(description = "다음 topic ID", example = "1")
        Integer topicId
) {
}
