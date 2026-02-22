package com.ktb.answer.dto.response.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "인터뷰 세션 상태 조회 응답")
public record InterviewSessionStateResponse(
        @JsonProperty("session_id")
        @Schema(description = "인터뷰 세션 ID", example = "10151031-858c-4158-93b9-a3f4b766975f")
        String sessionId,

        @JsonProperty("interview_type")
        @Schema(description = "면접 모드", example = "REAL_INTERVIEW")
        String interviewType,

        @Schema(description = "세션 상태(IN_PROGRESS/RETRYING/COMPLETED/FAILED/EXPIRED)", example = "IN_PROGRESS")
        String status,

        @JsonProperty("turn_count")
        @Schema(description = "누적 turn 수", example = "3")
        int turnCount,

        @JsonProperty("current_topic_id")
        @Schema(description = "현재 topic ID", nullable = true, example = "1")
        Integer currentTopicId,

        @JsonProperty("last_turn_type")
        @Schema(description = "마지막 turn 유형", nullable = true, example = "follow_up")
        String lastTurnType,

        @JsonProperty("last_question")
        @Schema(description = "마지막 질문 본문", nullable = true)
        String lastQuestion,

        @JsonProperty("is_final")
        @Schema(description = "최종 상태 여부", example = "false")
        boolean isFinal,

        @JsonProperty("expires_at")
        @Schema(description = "세션 만료 시각(ISO-8601)", nullable = true, example = "2026-02-21T21:36:04.209817")
        String expiresAt,

        @JsonProperty("interview_history")
        @Schema(description = "세션 누적 인터뷰 이력")
        List<InterviewHistoryResponse> interviewHistory
) {
}
