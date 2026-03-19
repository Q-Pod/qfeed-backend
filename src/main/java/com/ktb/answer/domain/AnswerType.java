package com.ktb.answer.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnswerType {

    PRACTICE_INTERVIEW("연습 면접", "/profile/records/%d"),

    REAL_INTERVIEW("실전 면접", "/profile/records/real/%d");

    private final String description;
    private final String deepLink;
}
