package com.ktb.notification.service.impl;

import com.ktb.notification.config.RabbitMQProperties;
import com.ktb.notification.domain.Campaign;
import com.ktb.notification.domain.NotificationOutbox;
import com.ktb.notification.repository.NotificationOutboxRepository;
import com.ktb.notification.service.CampaignScheduler;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringPollingCampaignScheduler implements CampaignScheduler {

    private static final int IMMEDIATE_THRESHOLD_SECONDS = 1;

    private final NotificationOutboxRepository outboxRepository;
    private final RabbitMQProperties rabbitMQProperties;

    @Override
    public void schedule(Campaign campaign, LocalDateTime scheduledAt) {
        if (isImmediate(scheduledAt)) {
            log.info("Campaign {} scheduled for immediate execution via outbox", campaign.getId());

            outboxRepository.save(NotificationOutbox.of(
                "campaign.execution",
                "Campaign",
                String.valueOf(campaign.getId()),
                "campaign-execution:" + campaign.getId(),
                rabbitMQProperties.getExchanges().getDirect(),
                rabbitMQProperties.getRoutingKeys().getCampaignExecution(),
                Map.of("campaignId", campaign.getId())
            ));
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
