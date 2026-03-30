package com.ktb.notification.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationOutbox {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true, length = 36)
    private String messageId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 200)
    private String aggregateId;

    @Column(name = "dedupe_key", length = 200)
    private String dedupeKey;

    @Column(name = "exchange", nullable = false, length = 100)
    private String exchange;

    @Column(name = "routing_key", nullable = false, length = 100)
    private String routingKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by", length = 200)
    private String lockedBy;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public static NotificationOutbox of(
            String eventType,
            String aggregateType,
            String aggregateId,
            String exchange,
            String routingKey,
            Map<String, Object> payload
    ) {
        return of(eventType, aggregateType, aggregateId, null, exchange, routingKey, payload);
    }

    public static NotificationOutbox of(
            String eventType,
            String aggregateType,
            String aggregateId,
            String dedupeKey,
            String exchange,
            String routingKey,
            Map<String, Object> payload
    ) {
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.messageId = UUID.randomUUID().toString();
        outbox.eventType = eventType;
        outbox.aggregateType = aggregateType;
        outbox.aggregateId = aggregateId;
        outbox.dedupeKey = dedupeKey;
        outbox.exchange = exchange;
        outbox.routingKey = routingKey;
        outbox.payload = serializePayload(payload);
        outbox.status = OutboxStatus.PENDING;
        outbox.retryCount = 0;
        outbox.maxRetries = 5;
        outbox.scheduledAt = Instant.now();
        outbox.createdAt = Instant.now();
        outbox.updatedAt = Instant.now();
        return outbox;
    }

    public void markProcessing(Instant now, String instanceId) {
        this.status = OutboxStatus.PROCESSING;
        this.lockedAt = now;
        this.lockedBy = instanceId;
        this.updatedAt = now;
    }

    public void markSent(Instant now) {
        this.status = OutboxStatus.SENT;
        this.sentAt = now;
        this.lockedAt = null;
        this.lockedBy = null;
        this.updatedAt = now;
    }

    public void markFailed(String errorMessage, Instant now) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = now;
    }

    public void scheduleRetry(Instant retryAt, String errorMessage) {
        this.retryCount++;
        this.status = retryCount >= maxRetries ? OutboxStatus.FAILED : OutboxStatus.PENDING;
        this.errorMessage = errorMessage;
        this.scheduledAt = retryAt;
        this.lockedAt = null;
        this.lockedBy = null;
        this.updatedAt = Instant.now();
    }

    public void recoverToPending(Instant now) {
        this.status = OutboxStatus.PENDING;
        this.lockedAt = null;
        this.lockedBy = null;
        this.updatedAt = now;
    }

    public Map<String, Object> getPayloadAsMap() {
        try {
            return MAPPER.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize outbox payload", e);
        }
    }

    private static String serializePayload(Map<String, Object> payload) {
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    public static final class OutboxStatus {
        public static final String PENDING = "PENDING";
        public static final String PROCESSING = "PROCESSING";
        public static final String SENT = "SENT";
        public static final String FAILED = "FAILED";

        private OutboxStatus() {}
    }
}
