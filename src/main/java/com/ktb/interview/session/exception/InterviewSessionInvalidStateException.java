package com.ktb.interview.session.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

/**
 * 현재 세션 상태에서 허용되지 않는 요청일 때 발생하는 예외.
 */
public class InterviewSessionInvalidStateException extends BusinessException {

    /**
     * 세션 상태 전이/요청 허용 범위 위반 메시지를 구성합니다.
     */
    public InterviewSessionInvalidStateException(String message) {
        super(ErrorCode.INTERVIEW_SESSION_INVALID_STATE, message);
    }
}
