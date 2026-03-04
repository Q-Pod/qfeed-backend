package com.ktb.notification.job;

import com.ktb.notification.event.CampaignExecutionEvent;
import com.ktb.notification.repository.CampaignRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignPoller {

    private final CampaignRepository campaignRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 10_000)
    public void pollAndExecute() {
        var readyToRun = campaignRepository.findReadyToExecute(LocalDateTime.now());

        if (readyToRun.isEmpty()) {
            return;
        }

        log.info("Poller found {} campaigns ready to execute", readyToRun.size());

        readyToRun.forEach(campaign ->
                eventPublisher.publishEvent(new CampaignExecutionEvent(this, campaign.getId()))
        );
    }
}
