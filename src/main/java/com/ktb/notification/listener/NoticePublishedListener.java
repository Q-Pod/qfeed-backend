package com.ktb.notification.listener;

import com.ktb.notification.domain.NotificationInbox;
import com.ktb.notification.domain.enums.TargetUsersType;
import com.ktb.notification.relay.OutboxMessage;
import com.ktb.notification.repository.NotificationInboxRepository;
import com.ktb.notification.service.CampaignPublishService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.LocalDateTime;
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
public class NoticePublishedListener {

    private final CampaignPublishService campaignPublishService;
    private final NotificationInboxRepository inboxRepository;
    private final TransactionTemplate transactionTemplate;

    @RabbitListener(
        queues = "${notification.rabbitmq.queues.notice-published}",
        containerFactory = "manualAckListenerContainerFactory"
    )
    public void onNoticePublished(
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
                Long noticeId = toLong(payload.get("noticeId"));
                Long campaignId = toLong(payload.get("campaignId"));
                String targetUsersStr = (String) payload.getOrDefault("targetUsers", "ALL");
                String scheduledAtStr = (String) payload.get("scheduledAt");
                LocalDateTime scheduledAt = LocalDateTime.parse(scheduledAtStr);
                TargetUsersType targetUsers = TargetUsersType.valueOf(targetUsersStr);

                campaignPublishService.publish(noticeId, campaignId, targetUsers, scheduledAt);

                inboxRepository.save(new NotificationInbox(
                    message.messageId(), message.eventType(), message.aggregateId()
                ));
            });

            channel.basicAck(deliveryTag, false);
            log.info("Notice published event processed messageId={}", message.messageId());

        } catch (DataIntegrityViolationException e) {
            log.debug("Inbox race condition resolved messageId={}", message.messageId());
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Failed to process notice published event messageId={}", message.messageId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number num) {
            return num.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
