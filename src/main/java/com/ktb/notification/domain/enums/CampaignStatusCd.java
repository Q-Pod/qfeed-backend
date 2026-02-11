package com.ktb.notification.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 캠페인 상태 코드
 */
@Getter
@RequiredArgsConstructor
public enum CampaignStatusCd {

    READY("대기"),
    RUNNING("실행 중"),
    COMPLETED("완료"),
    FAILED("실패"),
    CANCELLED("취소");

    private final String description;
}
