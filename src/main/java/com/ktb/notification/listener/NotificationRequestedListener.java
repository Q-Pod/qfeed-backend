package com.ktb.notification.listener;

import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.service.UserAccountService;
import com.ktb.notification.domain.NotificationInbox;
import com.ktb.notification.domain.NotificationTarget;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.relay.OutboxMessage;
import com.ktb.notification.repository.NotificationInboxRepository;
import com.ktb.notification.repository.NotificationTargetRepository;
import com.ktb.notification.service.UserNotificationService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
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
public class NotificationRequestedListener {

    private static final String DEDUPE_KEY_FORMAT = "%s:%s:%d";

    private final UserNotificationService userNotificationService;
    private final NotificationTargetRepository notificationTargetRepository;
    private final UserAccountService userAccountService;
    private final NotificationInboxRepository inboxRepository;
    private final TransactionTemplate transactionTemplate;

    @RabbitListener(
        queues = "${notification.rabbitmq.queues.notification-requested}",
        containerFactory = "manualAckListenerContainerFactory"
    )
    public void onNotificationRequested(
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
                processNotification(message);

                inboxRepository.save(new NotificationInbox(
                    message.messageId(), message.eventType(), message.aggregateId()
                ));
            });

            channel.basicAck(deliveryTag, false);
            log.debug("Notification request processed messageId={}", message.messageId());

        } catch (DataIntegrityViolationException e) {
            log.debug("Inbox race condition resolved messageId={}", message.messageId());
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Failed to process notification request messageId={}", message.messageId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void processNotification(OutboxMessage message) {
        Map<String, Object> payload = message.payload();
        Long targetAccountId = toLong(payload.get("targetAccountId"));
        String notificationCode = (String) payload.get("notificationCode");
        String title = (String) payload.getOrDefault("title", "");
        String body = (String) payload.getOrDefault("body", "");
        String deepLink = (String) payload.get("deepLink");
        String correlationId = (String) payload.get("correlationId");

        NotificationTypeCd type = resolveNotificationType(notificationCode);
        Long referenceId = tryParseLong(correlationId);
        String dedupeKey = String.format(DEDUPE_KEY_FORMAT, notificationCode, referenceId, targetAccountId);

        Optional<NotificationTarget> existing = notificationTargetRepository.findByDedupeKey(dedupeKey);
        if (existing.isPresent() && existing.get().isSent()) {
            log.info("Already delivered - dedupeKey={}", dedupeKey);
            return;
        }

        NotificationTarget target = existing.orElseGet(() -> {
            UserAccount account = userAccountService.findById(targetAccountId);
            return notificationTargetRepository.save(NotificationTarget.create(
                    account, null, title, body, deepLink, dedupeKey, referenceId
            ));
        });

        target.markAsQueued();
        userNotificationService.createNotification(targetAccountId, type, title, body, deepLink, referenceId);
        target.markAsSent();
    }

    private NotificationTypeCd resolveNotificationType(String code) {
        if ("answer_feedback_completed".equals(code)) {
            return NotificationTypeCd.ANSWER_FEEDBACK;
        }
        throw new IllegalArgumentException("Unsupported notification code: " + code);
    }

    private Long toLong(Object value) {
        if (value instanceof Number num) {
            return num.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Long tryParseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
