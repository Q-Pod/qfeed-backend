package com.ktb.notification.event;

import com.ktb.notification.service.CampaignPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticePublishedEventListener {

    private final CampaignPublishService campaignPublishService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNoticePublished(NoticePublishedEvent event) {
        log.info("Notice {} published, starting campaign {}", event.getNoticeId(), event.getCampaignId());
        campaignPublishService.publish(
                event.getNoticeId(),
                event.getCampaignId(),
                event.getTargetUsers(),
                event.getScheduledAt()
        );
    }
}
