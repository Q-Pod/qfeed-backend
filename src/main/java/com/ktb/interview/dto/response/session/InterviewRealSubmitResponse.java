package com.ktb.interview.dto.response.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ktb.interview.dto.ai.InterviewBadCaseFeedbackResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "실전 모드 답변 제출 응답")
public record InterviewRealSubmitResponse(
        @JsonProperty("session_id")
        @Schema(description = "인터뷰 세션 ID", example = "10151031-858c-4158-93b9-a3f4b766975f")
        String sessionId,

        @Schema(description = "세션 처리 상태", example = "IN_PROGRESS")
        String status,

        @JsonProperty("bad_case_feedback")
        @Schema(description = "Bad case 감지 정보(없으면 null)")
        InterviewBadCaseFeedbackResponse badCaseFeedback,

        @JsonProperty("next_question")
        @Schema(description = "다음 질문 정보(세션 종료 시 종료 메시지)")
        InterviewNextQuestionTurnResponse nextQuestion,

        @JsonProperty("is_final")
        @Schema(description = "면접 종료 여부(true면 최종 피드백 요청 가능)", example = "false")
        Boolean isFinal
) {
}
