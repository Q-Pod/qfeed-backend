package com.ktb.answer.dto.response.submit;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "실전 세션 기반 답변 제출 응답")
public record SessionAnswerSubmitResponse(

        @Schema(description = "생성된 답변 ID", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
        Long answerId,

        @Schema(description = "비디오 파일 URL (비디오 파일 제출 시)", example = "https://cdn.example.com/video/123.mp4")
        String videoUrl,

        @Schema(description = "즉각 피드백 (키워드 체크)", requiredMode = Schema.RequiredMode.REQUIRED)
        ImmediateFeedback immediateFeedback,

        @Schema(description = "AI 피드백 처리 상태", example = "processing",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"processing", "completed", "failed"})
        String aiFeedbackStatus,

        @Schema(description = "다음 질문 정보 (세션 계속 진행 시)")
        NextQuestion nextQuestion
) {
    private static final String DEFAULT_AI_FEEDBACK_STATUS = "processing";

    public static SessionAnswerSubmitResponse processing(
            Long answerId,
            ImmediateFeedback immediateFeedback,
            NextQuestion nextQuestion
    ) {
        return new SessionAnswerSubmitResponse(
                answerId,
                null,
                immediateFeedback,
                DEFAULT_AI_FEEDBACK_STATUS,
                nextQuestion
        );
    }
}
