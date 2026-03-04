package com.ktb.notification.service;

import com.ktb.notification.domain.Campaign;
import java.time.LocalDateTime;

public interface CampaignScheduler {

    void schedule(Campaign campaign, LocalDateTime scheduledAt);

    void cancel(Long campaignId);
}
