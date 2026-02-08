package com.ktb.answer.dto.response.detail;

import com.ktb.answer.dto.AiFeedbackSummary;
import com.ktb.answer.dto.AnswerDetailResult;
import com.ktb.answer.dto.response.common.KeywordCheck;
import com.ktb.answer.dto.response.common.MetricScore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "답변 상세 조회 응답")
public record AnswerDetailResponse(

        @Schema(description = "답변 ID", example = "1")
        Long answerId,

        @Schema(description = "답변 내용", example = "프로세스는 실행 중인 프로그램의 인스턴스이며...")
        String content,

        @Schema(description = "답변 타입", example = "PRACTICE_INTERVIEW")
        String type,

        @Schema(description = "답변 상태", example = "COMPLETED",
                allowableValues = {"SUBMITTED", "TRANSCRIBING", "IMMEDIATE_FEEDBACK_READY",
                                   "AI_FEEDBACK_PROCESSING", "COMPLETED", "FAILED_RETRYABLE", "FAILED"})
        String status,

        @Schema(description = "답변 작성 시각", example = "2026-01-22T10:30:00")
        String createdAt,

        @Schema(description = "질문 상세 정보 (expand=question 시 포함)")
        QuestionDetail question,

        @Schema(description = "즉각 피드백 정보 (expand=immediate_feedback 시 포함)")
        ImmediateFeedbackDetail immediateFeedback,

        @Schema(description = "AI 피드백 정보 (expand=feedback 시 포함)")
        AiFeedbackDetail aiFeedback
) {
    /**
     * AnswerDetailResult를 AnswerDetailResponse로 변환하는 정적 팩토리 메서드
     */
    public static AnswerDetailResponse of(AnswerDetailResult result) {
        QuestionDetail questionDetail = null;
        if (result.question() != null) {
            questionDetail = new QuestionDetail(
                result.question().questionId(),
                result.question().content(),
                result.question().category(),
                result.question().type(),
                null  // keywords는 별도 조회 필요
            );
        }

        ImmediateFeedbackDetail immediateFeedbackDetail = null;
        if (result.immediateFeedback() != null) {
            List<KeywordCheck> keywords = result.immediateFeedback().keywords().stream()
                .map(keyword -> new KeywordCheck(
                    keyword.keyword(),
                    keyword.included()
                ))
                .toList();
            immediateFeedbackDetail = new ImmediateFeedbackDetail(keywords);
        }

        AiFeedbackDetail aiFeedbackDetail = null;
        if (result.aiFeedback() != null) {
            AiFeedbackSummary summary = result.aiFeedback();
            List<MetricScore> metrics = summary.radarChart() == null
                ? null
                : summary.radarChart().entrySet().stream()
                    .map(entry -> new MetricScore(entry.getKey(), null, entry.getValue(), 5))
                    .toList();
            aiFeedbackDetail = new AiFeedbackDetail(
                summary.status() == null ? null : summary.status().name(),
                summary.comment(),
                metrics
            );
        }

        return new AnswerDetailResponse(
            result.answerId(),
            result.answer() == null ? null : result.answer().answerText(),
            result.type() == null ? null : result.type().name(),
            result.status() == null ? null : result.status().name(),
            result.answer() == null ? null : result.answer().answeredAt(),
            questionDetail,
            immediateFeedbackDetail,
            aiFeedbackDetail
        );
    }
}
