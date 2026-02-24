package com.ktb.interview.session.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "실전 모드 답변 제출 요청")
@JsonIgnoreProperties(ignoreUnknown = true)
public record RealAnswerSubmitRequest(
        @JsonAlias({"sessionId", "session_id"})
        @NotBlank(message = "sessionId is required")
        @Schema(description = "인터뷰 세션 ID", example = "10151031-858c-4158-93b9-a3f4b766975f")
        String sessionId,

        @JsonAlias({"answerText", "answer_text"})
        @NotBlank(message = "answerText is required")
        @Schema(
                description = "현재 질문에 대한 사용자 답변",
                example = "뮤텍스는 소유권 기반 잠금이고 세마포어는 카운트 기반 동기화입니다."
        )
        String answerText,

        @JsonAlias({"questionType", "question_type"})
        @Schema(description = "세션 questionType과 동기화 검증용(선택)", example = "CS")
        String questionType,

        @JsonAlias({"question", "question_text"})
        @NotBlank(message = "question is required")
        @Schema(
                description = "클라이언트가 인지한 현재 질문 텍스트(동기화 검증용)",
                example = "프로세스와 스레드의 차이점을 설명해주세요."
        )
        String question
) {
}
