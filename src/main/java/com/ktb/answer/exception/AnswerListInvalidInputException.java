package com.ktb.answer.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class AnswerListInvalidInputException extends BusinessException {

    public AnswerListInvalidInputException(String message) {
        super(ErrorCode.INVALID_INPUT, message);
    }

    public AnswerListInvalidInputException(String message, Throwable cause) {
        super(ErrorCode.INVALID_INPUT, message, cause);
    }
}
