package com.ktb.answer.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ktb.question.domain.QuestionCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "실전 모드 제출 시 동기화용 인터뷰 히스토리 turn")
public record RealInterviewHistoryTurnRequest(
        @Schema(description = "질문 텍스트", example = "프로세스와 스레드의 차이를 설명해주세요.")
        String question,

        @Schema(description = "질문 카테고리(선택)", nullable = true, example = "OS")
        QuestionCategory category,

        @JsonAlias({"answerText", "answer_text"})
        @Schema(description = "답변 텍스트", example = "프로세스는 독립 메모리 공간을 가지고 스레드는 프로세스 내부 자원을 공유합니다.")
        String answerText,

        @JsonAlias({"turnType", "turn_type"})
        @Schema(description = "turn 유형(new_topic/follow_up)", example = "follow_up")
        String turnType,

        @JsonAlias({"turnOrder", "turn_order"})
        @Schema(description = "turn 순서(0부터 시작)", example = "1")
        Integer turnOrder,

        @JsonAlias({"topicId", "topic_id"})
        @Schema(description = "토픽 ID", example = "1")
        Integer topicId
) {
}
