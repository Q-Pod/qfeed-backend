package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class CampaignKeyDuplicateException extends BusinessException {

    public CampaignKeyDuplicateException() {
        super(ErrorCode.CAMPAIGN_KEY_DUPLICATE);
    }

    public CampaignKeyDuplicateException(String campaignKey) {
        super(ErrorCode.CAMPAIGN_KEY_DUPLICATE,
            String.format(
                "%s: %s",
                ErrorCode.CAMPAIGN_KEY_DUPLICATE.getMessage(),
                campaignKey
            ));
    }
}
