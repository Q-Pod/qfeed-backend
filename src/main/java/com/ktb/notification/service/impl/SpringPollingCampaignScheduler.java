package com.ktb.notification.service.impl;

import com.ktb.notification.domain.Campaign;
import com.ktb.notification.event.CampaignExecutionEvent;
import com.ktb.notification.service.CampaignScheduler;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringPollingCampaignScheduler implements CampaignScheduler {

    private static final int IMMEDIATE_THRESHOLD_SECONDS = 1;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void schedule(Campaign campaign, LocalDateTime scheduledAt) {
        if (isImmediate(scheduledAt)) {
            log.info("Campaign {} scheduled for immediate execution", campaign.getId());

            // NoticePublishedEventListener.REQUIRES_NEW 커밋 후 @TransactionalEventListener 발화
            eventPublisher.publishEvent(new CampaignExecutionEvent(this, campaign.getId()));
        }
        // 예약 발송: READY 상태로 DB 저장 → CampaignPoller가 처리
    }

    @Override
    public void cancel(Long campaignId) {
        log.info("Campaign {} cancel requested (poller skips non-READY)", campaignId);
    }

    private boolean isImmediate(LocalDateTime scheduledAt) {
        return scheduledAt.isBefore(LocalDateTime.now().plusSeconds(IMMEDIATE_THRESHOLD_SECONDS));
    }
}
