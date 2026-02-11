package com.ktb.notification.domain;

import com.ktb.auth.domain.UserAccount;
import com.ktb.common.domain.BaseTimeEntity;
import com.ktb.notification.domain.enums.NotificationProviderCd;
import com.ktb.notification.domain.enums.NotificationTargetStatusCd;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "NOTIFICATION_TARGET",
        indexes = {
                @Index(name = "idx_target_campaign_status", columnList = "campaign_id, status_cd"),
                @Index(name = "idx_target_account", columnList = "account_id"),
                @Index(name = "idx_target_device", columnList = "device_id"),
                @Index(name = "idx_target_dedupe", columnList = "dedupe_key")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationTarget extends BaseTimeEntity {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int BODY_MAX_LENGTH = 500;
    private static final int DEEPLINK_MAX_LENGTH = 500;
    private static final int DEDUPE_KEY_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "noti_target_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private UserAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private UserDevice device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_cd", nullable = false, length = 20)
    private NotificationProviderCd provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_cd", nullable = false, length = 20)
    private NotificationTargetStatusCd status = NotificationTargetStatusCd.PENDING;

    @Column(name = "noti_title", nullable = false, length = 200)
    private String title;

    @Column(name = "noti_body", length = 500)
    private String body;

    @Column(name = "noti_deeplink", length = 500)
    private String deeplink;

    @Column(name = "dedupe_key", unique = true, length = 200)
    private String dedupeKey;

    @Column(name = "queued_at")
    private LocalDateTime queuedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Builder
    private NotificationTarget(
            UserAccount account,
            UserDevice device,
            Campaign campaign,
            NotificationProviderCd provider,
            String title,
            String body,
            String deeplink,
            String dedupeKey
    ) {
        this.account = account;
        this.device = device;
        this.campaign = campaign;
        this.provider = provider;
        this.title = title;
        this.body = body;
        this.deeplink = deeplink;
        this.dedupeKey = dedupeKey;
        this.status = NotificationTargetStatusCd.PENDING;
    }

    public static NotificationTarget create(
            UserAccount account,
            UserDevice device,
            Campaign campaign,
            NotificationProviderCd provider,
            String title,
            String body,
            String deeplink,
            String dedupeKey
    ) {
        return NotificationTarget.builder()
                .account(account)
                .device(device)
                .campaign(campaign)
                .provider(provider)
                .title(title)
                .body(body)
                .deeplink(deeplink)
                .dedupeKey(dedupeKey)
                .build();
    }

    public void markAsQueued() {
        this.status = NotificationTargetStatusCd.RUNNING;
        this.queuedAt = LocalDateTime.now();
    }

    public void markAsSent() {
        this.status = NotificationTargetStatusCd.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = NotificationTargetStatusCd.FAILED;
    }

    public void markAsSkipped() {
        this.status = NotificationTargetStatusCd.SKIPPED;
    }

    public boolean isPending() {
        return this.status == NotificationTargetStatusCd.PENDING;
    }

    public boolean isRunning() {
        return this.status == NotificationTargetStatusCd.RUNNING;
    }

    public boolean isSent() {
        return this.status == NotificationTargetStatusCd.SENT;
    }

    public boolean isFailed() {
        return this.status == NotificationTargetStatusCd.FAILED;
    }

    public boolean isSkipped() {
        return this.status == NotificationTargetStatusCd.SKIPPED;
    }

    public boolean isCompleted() {
        return isSent() || isFailed() || isSkipped();
    }
}
