package com.ktb.interview.session.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

/**
 * 만료된 세션 접근 시 발생하는 예외.
 */
public class InterviewSessionExpiredException extends BusinessException {

    private static final String MESSAGE_TEMPLATE = "%s sessionId=%s";

    /**
     * 만료된 세션 접근 오류 메시지를 구성합니다.
     */
    public InterviewSessionExpiredException(String sessionId) {
        super(ErrorCode.INTERVIEW_SESSION_EXPIRED,
                String.format(MESSAGE_TEMPLATE, ErrorCode.INTERVIEW_SESSION_EXPIRED.getMessage(), sessionId));
    }
}
