package com.ktb.answer.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "실전 모드 답변 제출 요청")
@JsonIgnoreProperties(ignoreUnknown = true)
public record RealAnswerSubmitRequest(
        @JsonAlias({"sessionId", "session_id"})
        @NotBlank(message = "sessionId is required")
        @Schema(description = "인터뷰 세션 ID", example = "10151031-858c-4158-93b9-a3f4b766975f")
        String sessionId,

        @JsonAlias({"questionId", "question_id"})
        @Schema(description = "실전 모드에서는 전달 금지", nullable = true)
        String questionId,

        @JsonAlias({"answerText", "answer_text"})
        @Schema(
                description = "직접 답변 텍스트를 전달할 때 사용(interview_history 미사용 시 필수)",
                example = "뮤텍스는 소유권 기반 잠금이고 세마포어는 카운트 기반 동기화입니다."
        )
        String answerText,

        @JsonAlias({"questionType", "question_type"})
        @Schema(description = "세션 questionType과 동기화 검증용(선택)", example = "CS")
        String questionType,

        @Schema(description = "AI follow-up 요청 카테고리 오버라이드(선택)", example = "OS")
        String category,

        @JsonAlias({"interviewHistory", "interview_history"})
        @Schema(
                description = "누적 인터뷰 이력(선택). 제공 시 마지막 turn의 answer_text를 현재 답변으로 사용",
                nullable = true
        )
        List<RealInterviewHistoryTurnRequest> interviewHistory
) {
    public String resolvedAnswerText() {
        if (answerText != null && !answerText.isBlank()) {
            return answerText;
        }
        if (interviewHistory == null || interviewHistory.isEmpty()) {
            return null;
        }
        RealInterviewHistoryTurnRequest last = interviewHistory.get(interviewHistory.size() - 1);
        return last == null ? null : last.answerText();
    }

    public String latestQuestionText() {
        if (interviewHistory == null || interviewHistory.isEmpty()) {
            return null;
        }
        RealInterviewHistoryTurnRequest last = interviewHistory.get(interviewHistory.size() - 1);
        return last == null ? null : last.question();
    }
}
