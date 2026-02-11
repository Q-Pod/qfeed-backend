package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class CampaignNotFoundException extends BusinessException {

    public CampaignNotFoundException() {
        super(ErrorCode.CAMPAIGN_NOT_FOUND);
    }

    public CampaignNotFoundException(Long campaignId) {
        super(ErrorCode.CAMPAIGN_NOT_FOUND,
            String.format(
                "%s ID: %d",
                ErrorCode.CAMPAIGN_NOT_FOUND.getMessage(),
                campaignId
            ));
    }
}
