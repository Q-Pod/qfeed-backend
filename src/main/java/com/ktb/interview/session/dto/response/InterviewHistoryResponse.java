package com.ktb.interview.session.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "세션 내 단일 인터뷰 turn 이력")
public record InterviewHistoryResponse(
        @Schema(description = "질문 본문")
        String question,

        @Schema(description = "질문 카테고리", nullable = true, example = "OS")
        String category,

        @JsonProperty("answer_text")
        @Schema(description = "답변 본문")
        String answerText,

        @JsonProperty("turn_type")
        @Schema(description = "turn 유형(new_topic/follow_up)")
        String turnType,

        @JsonProperty("turn_order")
        @Schema(description = "turn 순서", example = "0")
        int turnOrder,

        @JsonProperty("topic_id")
        @Schema(description = "topic ID", example = "1")
        Integer topicId,

        @JsonProperty("video_file_id")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "실전모드 turn 영상 파일 ID", nullable = true, example = "12345")
        Long videoFileId,

        @JsonProperty("video_play_url")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "실전모드 turn 영상 재생용 Presigned GET URL", nullable = true,
                example = "https://example-bucket.s3.ap-northeast-2.amazonaws.com/video/...")
        String videoPlayUrl
) {
}
