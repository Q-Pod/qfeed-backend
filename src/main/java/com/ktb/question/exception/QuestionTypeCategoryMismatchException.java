package com.ktb.question.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class QuestionTypeCategoryMismatchException extends BusinessException {

    public QuestionTypeCategoryMismatchException() {
        super(ErrorCode.QUESTION_TYPE_CATEGORY_MISMATCH);
    }
}
