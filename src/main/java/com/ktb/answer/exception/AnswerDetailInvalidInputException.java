package com.ktb.answer.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class AnswerDetailInvalidInputException extends BusinessException {

    public AnswerDetailInvalidInputException(String message) {
        super(ErrorCode.INVALID_INPUT, message);
    }
}
