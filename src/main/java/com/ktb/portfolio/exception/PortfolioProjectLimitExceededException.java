package com.ktb.portfolio.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;


public class PortfolioProjectLimitExceededException extends BusinessException {
    public PortfolioProjectLimitExceededException() {
        super(ErrorCode.PORTFOLIO_PROJECT_LIMIT_EXCEEDED);
    }
}
