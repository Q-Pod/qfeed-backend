package com.ktb.answer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record AnswerListCursor(
        @JsonProperty("last_created_at") LocalDateTime lastCreatedAt,
        @JsonProperty("last_answer_id") Long lastAnswerId
) {
}
