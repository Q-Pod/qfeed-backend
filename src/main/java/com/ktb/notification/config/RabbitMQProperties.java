package com.ktb.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "notification.rabbitmq")
public class RabbitMQProperties {

    private Exchanges exchanges = new Exchanges();
    private Queues queues = new Queues();
    private RoutingKeys routingKeys = new RoutingKeys();
    private boolean enabled = false;

    @Getter
    @Setter
    public static class Exchanges {
        private String direct = "notification.direct";
        private String dlx = "notification.dlx";
    }

    @Getter
    @Setter
    public static class Queues {
        private String campaignExecution = "notification.campaign-execution";
        private String notificationRequested = "notification.notification-requested";
        private String noticePublished = "notification.notice-published";
        private String dlqCampaignExecution = "notification.dlq.campaign-execution";
        private String dlqNotificationRequested = "notification.dlq.notification-requested";
        private String dlqNoticePublished = "notification.dlq.notice-published";
    }

    @Getter
    @Setter
    public static class RoutingKeys {
        private String campaignExecution = "campaign.execution";
        private String notificationRequested = "notification.requested";
        private String noticePublished = "notice.published";
        private String dlqCampaignExecution = "dlq.campaign.execution";
        private String dlqNotificationRequested = "dlq.notification.requested";
        private String dlqNoticePublished = "dlq.notice.published";
    }
}
