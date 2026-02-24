package com.ktb.interview.session.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

/**
 * 인터뷰 세션 설정값이 유효하지 않을 때 발생하는 예외.
 */
public class InterviewSessionInvalidConfigException extends BusinessException {

    /**
     * 세션 설정 검증 실패 메시지를 구성합니다.
     */
    public InterviewSessionInvalidConfigException(String message) {
        super(ErrorCode.INTERVIEW_SESSION_INVALID_CONFIG, message);
    }
}
