package com.ktb.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "notification.outbox")
public class OutboxRelayProperties {

    private int batchSize = 100;
    private long relayIntervalMs = 1_000;
    private long staleThresholdSeconds = 300;
    private long inboxRetentionDays = 7;
}
