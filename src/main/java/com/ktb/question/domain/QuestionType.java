package com.ktb.question.domain;

import lombok.Getter;

/**
 * 질문 종류 Enum
 */
@Getter
public enum QuestionType {
    CS("CS"),
    SYSTEM_DESIGN("시스템 디자인"),
    PORTFOLIO("개인화");

    private final String type;

    QuestionType(String type) {
        this.type = type;
    }
}
