package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class CampaignValidationException extends BusinessException {

    public CampaignValidationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
