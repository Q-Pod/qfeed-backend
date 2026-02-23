package com.ktb.interview.dto.response.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인터뷰 세션 생성 응답")
public record InterviewSessionCreateResponse(
        @JsonProperty("session_id")
        @Schema(description = "인터뷰 세션 ID", example = "10151031-858c-4158-93b9-a3f4b766975f")
        String sessionId,

        @JsonProperty("interview_type")
        @Schema(description = "면접 모드", example = "REAL_INTERVIEW")
        String interviewType,

        @JsonProperty("question_type")
        @Schema(description = "질문 유형", example = "CS")
        String questionType,

        @JsonProperty("question_text")
        @Schema(description = "현재 질문 텍스트(실전: AI 첫 질문, 연습: null)", nullable = true)
        String questionText,

        @Schema(description = "카테고리", nullable = true, example = "OS")
        String category,

        @JsonProperty("turn_type")
        @Schema(description = "현재 turn 유형", example = "main")
        String turnType,

        @JsonProperty("topic_id")
        @Schema(description = "현재 topic ID", nullable = true, example = "1")
        Integer topicId,

        @JsonProperty("expires_at")
        @Schema(description = "세션 만료 시각(ISO-8601)", example = "2026-02-21T21:36:04.209817")
        String expiresAt
) {
}
