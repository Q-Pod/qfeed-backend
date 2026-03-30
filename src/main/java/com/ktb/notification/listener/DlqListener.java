package com.ktb.notification.listener;

import com.ktb.notification.relay.OutboxMessage;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.rabbitmq.enabled", havingValue = "true")
public class DlqListener {

    @RabbitListener(
        queues = {
            "${notification.rabbitmq.queues.dlq-campaign-execution}",
            "${notification.rabbitmq.queues.dlq-notification-requested}",
            "${notification.rabbitmq.queues.dlq-notice-published}"
        },
        containerFactory = "manualAckListenerContainerFactory"
    )
    public void onDlqMessage(
            Message rawMessage,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = AmqpHeaders.CONSUMER_QUEUE, required = false) String queue)
            throws IOException {

        String messageId = rawMessage.getMessageProperties().getMessageId();
        log.error("[DLQ] Received dead-letter message from queue={} messageId={}",
            queue, messageId);

        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("[DLQ] Failed to ACK dead-letter message messageId={}", messageId, e);
            throw e;
        }
    }
}
