package com.ktb.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "notification_delivery_attempt",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_delivery_attempt",
        columnNames = {"notification_target_id", "attempt_no"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDeliveryAttempt {

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_RETRYABLE = "RETRYABLE";
    private static final String RESULT_PERMANENT = "PERMANENT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_target_id", nullable = false)
    private NotificationTarget notificationTarget;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "result_cd", nullable = false, length = 20)
    private String resultCd;

    @Column(name = "provider_error_cd", length = 100)
    private String providerErrorCd;

    @Column(name = "provider_error_msg", columnDefinition = "text")
    private String providerErrorMsg;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    public static NotificationDeliveryAttempt success(
            NotificationTarget target, int attemptNo, Integer latencyMs) {
        NotificationDeliveryAttempt attempt = new NotificationDeliveryAttempt();
        attempt.notificationTarget = target;
        attempt.attemptNo = attemptNo;
        attempt.resultCd = RESULT_SUCCESS;
        attempt.latencyMs = latencyMs;
        attempt.attemptedAt = Instant.now();
        return attempt;
    }

    public static NotificationDeliveryAttempt retryable(
            NotificationTarget target, int attemptNo, String errorCd, String errorMsg) {
        NotificationDeliveryAttempt attempt = new NotificationDeliveryAttempt();
        attempt.notificationTarget = target;
        attempt.attemptNo = attemptNo;
        attempt.resultCd = RESULT_RETRYABLE;
        attempt.providerErrorCd = errorCd;
        attempt.providerErrorMsg = errorMsg;
        attempt.attemptedAt = Instant.now();
        return attempt;
    }

    public static NotificationDeliveryAttempt permanent(
            NotificationTarget target, int attemptNo, String errorCd, String errorMsg) {
        NotificationDeliveryAttempt attempt = new NotificationDeliveryAttempt();
        attempt.notificationTarget = target;
        attempt.attemptNo = attemptNo;
        attempt.resultCd = RESULT_PERMANENT;
        attempt.providerErrorCd = errorCd;
        attempt.providerErrorMsg = errorMsg;
        attempt.attemptedAt = Instant.now();
        return attempt;
    }
}
