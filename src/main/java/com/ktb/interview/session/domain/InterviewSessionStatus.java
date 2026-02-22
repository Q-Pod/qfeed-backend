package com.ktb.interview.session.domain;

/**
 * 인터뷰 세션 상태.
 */
public enum InterviewSessionStatus {
    IN_PROGRESS,
    RETRYING,
    COMPLETED,
    FAILED,
    EXPIRED
}
