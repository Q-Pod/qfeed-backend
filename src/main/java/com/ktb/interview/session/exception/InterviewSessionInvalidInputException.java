package com.ktb.interview.session.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

/**
 * 인터뷰 세션 요청 입력값이 유효하지 않을 때 발생하는 예외.
 */
public class InterviewSessionInvalidInputException extends BusinessException {

    /**
     * 세션 입력값 검증 실패 메시지를 구성합니다.
     */
    public InterviewSessionInvalidInputException(String message) {
        super(ErrorCode.INTERVIEW_SESSION_INVALID_INPUT, message);
    }
}
