package com.ktb.notification.listener;

import com.ktb.notification.domain.Campaign;
import com.ktb.notification.domain.NotificationInbox;
import com.ktb.notification.domain.NotificationTarget;
import com.ktb.notification.domain.enums.NotificationTargetStatusCd;
import com.ktb.notification.exception.CampaignInvalidStatusTransitionException;
import com.ktb.notification.exception.CampaignNotFoundException;
import com.ktb.notification.relay.OutboxMessage;
import com.ktb.notification.repository.CampaignRepository;
import com.ktb.notification.repository.NotificationInboxRepository;
import com.ktb.notification.repository.NotificationTargetRepository;
import com.ktb.notification.service.UserNotificationService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.rabbitmq.enabled", havingValue = "true")
public class CampaignExecutionListener {

    private final CampaignRepository campaignRepository;
    private final NotificationTargetRepository notificationTargetRepository;
    private final UserNotificationService userNotificationService;
    private final NotificationInboxRepository inboxRepository;
    private final TransactionTemplate transactionTemplate;

    @RabbitListener(
        queues = "${notification.rabbitmq.queues.campaign-execution}",
        containerFactory = "manualAckListenerContainerFactory"
    )
    public void onCampaignExecution(
            @Payload OutboxMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        if (inboxRepository.existsByMessageId(message.messageId())) {
            log.debug("Duplicate message skipped messageId={}", message.messageId());
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            transactionTemplate.executeWithoutResult(status -> {
                Map<String, Object> payload = message.payload();
                Long campaignId = toLong(payload.get("campaignId"));

                processCampaign(campaignId);

                inboxRepository.save(new NotificationInbox(
                    message.messageId(), message.eventType(), message.aggregateId()
                ));
            });

            channel.basicAck(deliveryTag, false);
            log.debug("Campaign execution processed messageId={}", message.messageId());

        } catch (DataIntegrityViolationException e) {
            log.debug("Inbox race condition resolved messageId={}", message.messageId());
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Failed to process campaign execution messageId={}", message.messageId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
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

    private Long toLong(Object value) {
        if (value instanceof Number num) {
            return num.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
