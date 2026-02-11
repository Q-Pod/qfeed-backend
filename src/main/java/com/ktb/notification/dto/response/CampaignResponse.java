package com.ktb.notification.dto.response;

import com.ktb.notification.domain.Campaign;
import com.ktb.notification.domain.enums.CampaignStatusCd;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import java.time.LocalDateTime;

public record CampaignResponse(
        Long id,
        NotificationTypeCd campaignType,
        String campaignTypeDescription,
        String campaignKey,
        CampaignStatusCd status,
        String statusDescription,
        LocalDateTime scheduledAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {

    public static CampaignResponse from(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getCampaignType(),
                campaign.getCampaignType().getDescription(),
                campaign.getCampaignKey(),
                campaign.getStatus(),
                campaign.getStatus().getDescription(),
                campaign.getScheduledAt(),
                campaign.getStartedAt(),
                campaign.getCompletedAt(),
                campaign.getCreatedAt()
        );
    }
}
