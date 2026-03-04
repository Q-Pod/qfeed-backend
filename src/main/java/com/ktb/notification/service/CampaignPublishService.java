package com.ktb.notification.service;

import com.ktb.notification.domain.enums.TargetUsersType;
import java.time.LocalDateTime;

public interface CampaignPublishService {

    void publish(Long noticeId, Long campaignId, TargetUsersType targetUsersType, LocalDateTime scheduledAt);
}
