package com.ktb.notification.job;

import com.ktb.notification.config.RabbitMQProperties;
import com.ktb.notification.domain.NotificationOutbox;
import com.ktb.notification.repository.CampaignRepository;
import com.ktb.notification.repository.NotificationOutboxRepository;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignPoller {

    private final CampaignRepository campaignRepository;
    private final NotificationOutboxRepository outboxRepository;
    private final RabbitMQProperties rabbitMQProperties;

    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void pollAndExecute() {
        var readyToRun = campaignRepository.findReadyToExecute(LocalDateTime.now());

        if (readyToRun.isEmpty()) {
            return;
        }

        log.info("Poller found {} campaigns ready to execute", readyToRun.size());

        readyToRun.forEach(campaign -> {
            NotificationOutbox outbox = NotificationOutbox.of(
                "campaign.execution",
                "Campaign",
                String.valueOf(campaign.getId()),
                "campaign-execution:" + campaign.getId(),
                rabbitMQProperties.getExchanges().getDirect(),
                rabbitMQProperties.getRoutingKeys().getCampaignExecution(),
                Map.of("campaignId", campaign.getId())
            );
            outboxRepository.save(outbox);
        });
    }
}
