package com.ktb.interview.session.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

/**
 * 세션 소유자가 아닌 사용자의 접근 시 발생하는 예외.
 */
public class InterviewSessionAccessDeniedException extends BusinessException {

    /**
     * 세션 소유권 검증 실패 메시지를 구성합니다.
     */
    public InterviewSessionAccessDeniedException(String sessionId, Long accountId) {
        super(ErrorCode.INTERVIEW_SESSION_ACCESS_DENIED,
                ErrorCode.INTERVIEW_SESSION_ACCESS_DENIED.getMessage()
                        + " sessionId=" + sessionId + ", accountId=" + accountId);
    }
}
