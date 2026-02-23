package com.ktb.interview.session.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

/**
 * 세션 식별자로 조회한 인터뷰 세션이 없을 때 발생하는 예외.
 */
public class InterviewSessionNotFoundException extends BusinessException {

    private static final String MESSAGE_TEMPLATE = "%s sessionId=%s";

    /**
     * 세션 미존재 오류 메시지를 구성합니다.
     */
    public InterviewSessionNotFoundException(String sessionId) {
        super(ErrorCode.INTERVIEW_SESSION_NOT_FOUND,
                String.format(MESSAGE_TEMPLATE, ErrorCode.INTERVIEW_SESSION_NOT_FOUND.getMessage(), sessionId));
    }
}
