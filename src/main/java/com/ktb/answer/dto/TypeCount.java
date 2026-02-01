package com.ktb.answer.dto;

import com.ktb.answer.domain.AnswerType;

public record TypeCount(
        AnswerType type,
        long count
) {
}
