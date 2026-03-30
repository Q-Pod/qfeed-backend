package com.ktb.notification.relay;

import com.ktb.notification.config.OutboxRelayProperties;
import com.ktb.notification.config.RabbitMQProperties;
import com.ktb.notification.domain.NotificationOutbox;
import com.ktb.notification.repository.NotificationOutboxRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.rabbitmq.enabled", havingValue = "true")
public class OutboxRelayService {

    private final NotificationOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final OutboxRelayProperties relayProps;
    private final RabbitMQProperties mqProps;

    private final String instanceId = UUID.randomUUID().toString();

    @Scheduled(fixedDelayString = "${notification.outbox.relay-interval-ms:1000}")
    @SchedulerLock(name = "outbox-relay", lockAtMostFor = "PT30S", lockAtLeastFor = "PT1S")
    @Transactional
    public void relay() {
        List<NotificationOutbox> pending = outboxRepository
            .findPendingToRelay(Instant.now(), relayProps.getBatchSize());

        if (pending.isEmpty()) {
            return;
        }

        List<Long> ids = pending.stream().map(NotificationOutbox::getId).toList();
        outboxRepository.markProcessingByIds(ids, Instant.now(), instanceId);

        log.debug("Relaying {} outbox messages", pending.size());

        for (NotificationOutbox outbox : pending) {
            try {
                sendToRabbitMQ(outbox);
            } catch (Exception e) {
                log.error("Failed to relay outbox message={}, retryCount={}",
                    outbox.getMessageId(), outbox.getRetryCount(), e);
                Instant retryAt = computeRetryAt(outbox.getRetryCount());
                outbox.scheduleRetry(retryAt, e.getMessage());
                outboxRepository.save(outbox);
            }
        }
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void recoverStaleProcessing() {
        Instant staleBefore = Instant.now()
            .minusSeconds(relayProps.getStaleThresholdSeconds());
        int recovered = outboxRepository.recoverStaleProcessing(staleBefore, Instant.now());
        if (recovered > 0) {
            log.info("Recovered {} stale PROCESSING outbox records", recovered);
        }
    }

    private void sendToRabbitMQ(NotificationOutbox outbox) {
        OutboxMessage message = OutboxMessage.from(outbox);

        rabbitTemplate.convertAndSend(
            outbox.getExchange(),
            outbox.getRoutingKey(),
            message,
            msg -> {
                msg.getMessageProperties().setMessageId(outbox.getMessageId());
                msg.getMessageProperties().setContentType("application/json");
                return msg;
            }
        );

        outboxRepository.markSentByMessageId(outbox.getMessageId(), Instant.now());
        log.debug("Relayed outbox message={} routing={}", outbox.getMessageId(), outbox.getRoutingKey());
    }

    private Instant computeRetryAt(int currentRetryCount) {
        long delaySeconds = switch (currentRetryCount) {
            case 0 -> 1;
            case 1 -> 5;
            case 2 -> 30;
            case 3 -> 120;
            default -> 300;
        };
        return Instant.now().plusSeconds(delaySeconds);
    }
}
