package com.ktb.notification.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공지사항 상태 코드
 */
@Getter
@RequiredArgsConstructor
public enum NoticeStatusCd {

    DRAFT("작성 중"),
    PUBLISHED("발행됨"),
    COMPLETED("발송 완료"),
    CANCELLED("취소됨");

    private final String description;
}
