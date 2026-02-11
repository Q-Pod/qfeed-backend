package com.ktb.notification.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;
import com.ktb.notification.domain.enums.CampaignStatusCd;

public class CampaignInvalidStatusTransitionException extends BusinessException {

    public CampaignInvalidStatusTransitionException() {
        super(ErrorCode.CAMPAIGN_INVALID_STATUS_TRANSITION);
    }

    public CampaignInvalidStatusTransitionException(CampaignStatusCd from, CampaignStatusCd to) {
        super(ErrorCode.CAMPAIGN_INVALID_STATUS_TRANSITION,
                String.format(
                    "%s, 상태 전이 불가: %s → %s",
                    ErrorCode.CAMPAIGN_INVALID_STATUS_TRANSITION.getMessage(),
                    from.getDescription(),
                    to.getDescription()
                ));
    }
}
