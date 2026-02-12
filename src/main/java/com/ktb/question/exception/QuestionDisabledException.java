package com.ktb.question.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class QuestionDisabledException extends BusinessException {

    public QuestionDisabledException(Long questionId) {
        super(
                ErrorCode.QUESTION_DISABLED,
                String.format("%s: %d", ErrorCode.QUESTION_DISABLED.getMessage(), questionId)
        );
    }
}
