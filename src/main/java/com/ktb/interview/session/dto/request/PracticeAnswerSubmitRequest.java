package com.ktb.interview.session.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "연습 모드 답변 제출 요청")
public record PracticeAnswerSubmitRequest(
        @NotBlank(message = "sessionId is required")
        @Schema(description = "인터뷰 세션 ID", example = "2371d491-c5eb-4650-af9f-3fab24bdc72b")
        String sessionId,

        @NotNull(message = "questionId is required")
        @Schema(description = "연습 모드 질문 ID", example = "1001")
        Long questionId,

        @NotBlank(message = "answerText is required")
        @Size(min = 2, max = 1500, message = "answerText must be between 2 and 1500 characters")
        @Schema(description = "사용자 답변 본문(2~1500자)", example = "프로세스는 독립 메모리 공간을 가지며 스레드는 프로세스 내부 자원을 공유합니다.")
        String answerText
) {
}
