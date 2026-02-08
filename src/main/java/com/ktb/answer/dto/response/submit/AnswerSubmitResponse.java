package com.ktb.answer.dto.response.submit;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "답변 제출 응답 (연습/단일 답변)")
public record AnswerSubmitResponse(

        @Schema(description = "생성된 답변 ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
        Long answerId,

        @Schema(description = "즉각 피드백 (키워드 체크)", requiredMode = Schema.RequiredMode.REQUIRED)
        ImmediateFeedback immediateFeedback,

        @Schema(description = "AI 피드백 처리 상태", example = "processing",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"processing", "completed", "failed"})
        String aiFeedbackStatus
) {
    private static final String DEFAULT_AI_FEEDBACK_STATUS = "processing";

    public static AnswerSubmitResponse processing(Long answerId, ImmediateFeedback immediateFeedback) {
        return new AnswerSubmitResponse(
                answerId,
                immediateFeedback,
                DEFAULT_AI_FEEDBACK_STATUS
        );
    }
}
