package com.ktb.notification.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 유형 코드
 */
@Getter
@RequiredArgsConstructor
public enum NotificationTypeCd {

    NOTICE("공지사항"),
    REVISIT("재방문 유도"),
    ANSWER_FEEDBACK("답변 피드백"),
    PROJECT_REMINDER("프로젝트 독려");

    private final String description;
}
