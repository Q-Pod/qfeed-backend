package com.ktb.notification.worker;

import com.ktb.async.core.worker.AbstractEventWorker;
import com.ktb.notification.domain.Campaign;
import com.ktb.notification.domain.NotificationTarget;
import com.ktb.notification.domain.enums.NotificationTargetStatusCd;
import com.ktb.notification.event.CampaignExecutionEvent;
import com.ktb.notification.exception.CampaignInvalidStatusTransitionException;
import com.ktb.notification.exception.CampaignNotFoundException;
import com.ktb.notification.repository.CampaignRepository;
import com.ktb.notification.repository.NotificationTargetRepository;
import com.ktb.notification.service.UserNotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignExecutionWorker extends AbstractEventWorker<CampaignExecutionEvent> {

    private final CampaignRepository campaignRepository;
    private final NotificationTargetRepository notificationTargetRepository;
    private final UserNotificationService userNotificationService;
    private final PlatformTransactionManager transactionManager;

    @Async("notificationWorkerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCampaignExecution(CampaignExecutionEvent event) {
        handle(event);
    }

    @Override
    protected void process(CampaignExecutionEvent event) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        txTemplate.execute(status -> {
            processCampaign(event.getCampaignId());
            return null;
        });
    }

    private void processCampaign(Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

        try {
            campaign.start();
        } catch (CampaignInvalidStatusTransitionException e) {
            log.warn("Campaign {} already started, skipping duplicate execution (status={})",
                    campaignId, campaign.getStatus());
            return;
        }

        List<NotificationTarget> targets = notificationTargetRepository
                .findByCampaignIdAndStatus(campaignId, NotificationTargetStatusCd.PENDING);

        int sent = 0;
        int failed = 0;

        for (NotificationTarget target : targets) {
            try {
                target.markAsQueued();
                userNotificationService.createNotification(
                        target.getAccount().getId(),
                        campaign.getCampaignType(),
                        target.getTitle(),
                        target.getBody(),
                        target.getDeeplink(),
                        target.getReferenceId()
                );
                target.markAsSent();
                sent++;
            } catch (Exception e) {
                log.error("Failed to deliver notification for target {} (accountId={})",
                        target.getId(), target.getAccount().getId(), e);
                target.markAsFailed();
                failed++;
            }
        }

        campaign.complete();
        log.info("Campaign {} completed — sent={}, failed={}", campaignId, sent, failed);
    }
}
