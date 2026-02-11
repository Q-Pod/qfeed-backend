package com.ktb.notification.domain;

import com.ktb.common.domain.BaseTimeEntity;
import com.ktb.notification.domain.enums.CampaignStatusCd;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import com.ktb.common.domain.ErrorCode;
import com.ktb.notification.exception.CampaignInvalidStatusTransitionException;
import com.ktb.notification.exception.CampaignValidationException;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "CAMPAIGN",
        indexes = {
                @Index(name = "idx_status_scheduled", columnList = "campaign_status_cd, campaign_scheduled_at"),
                @Index(name = "idx_type", columnList = "campaign_type_cd"),
                @Index(name = "idx_key", columnList = "campaign_key")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Campaign extends BaseTimeEntity {

    private static final int KEY_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_type_cd", nullable = false, length = 50)
    private NotificationTypeCd campaignType;

    @Column(name = "campaign_key", nullable = false, unique = true, length = 200)
    private String campaignKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_status_cd", nullable = false, length = 20)
    private CampaignStatusCd status = CampaignStatusCd.READY;

    @Column(name = "campaign_scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "campaign_started_at")
    private LocalDateTime startedAt;

    @Column(name = "campaign_completed_at")
    private LocalDateTime completedAt;

    @Builder
    private Campaign(
            NotificationTypeCd campaignType,
            String campaignKey,
            LocalDateTime scheduledAt
    ) {
        validateCampaignKey(campaignKey);
        validateScheduledAt(scheduledAt);
        this.campaignType = campaignType;
        this.campaignKey = campaignKey;
        this.scheduledAt = scheduledAt;
        this.status = CampaignStatusCd.READY;
    }

    public static Campaign create(
            NotificationTypeCd campaignType,
            String campaignKey,
            LocalDateTime scheduledAt
    ) {
        return Campaign.builder()
                .campaignType(campaignType)
                .campaignKey(campaignKey)
                .scheduledAt(scheduledAt)
                .build();
    }

    public void start() {
        if (this.status != CampaignStatusCd.READY) {
            throw new CampaignInvalidStatusTransitionException(this.status, CampaignStatusCd.RUNNING);
        }
        this.status = CampaignStatusCd.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        if (this.status != CampaignStatusCd.RUNNING) {
            throw new CampaignInvalidStatusTransitionException(this.status, CampaignStatusCd.COMPLETED);
        }
        this.status = CampaignStatusCd.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail() {
        if (this.status != CampaignStatusCd.RUNNING) {
            throw new CampaignInvalidStatusTransitionException(this.status, CampaignStatusCd.FAILED);
        }
        this.status = CampaignStatusCd.FAILED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == CampaignStatusCd.COMPLETED || this.status == CampaignStatusCd.FAILED) {
            throw new CampaignInvalidStatusTransitionException(this.status, CampaignStatusCd.CANCELLED);
        }
        this.status = CampaignStatusCd.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    public boolean isReady() {
        return this.status == CampaignStatusCd.READY;
    }

    public boolean isRunning() {
        return this.status == CampaignStatusCd.RUNNING;
    }

    public boolean isCompleted() {
        return this.status == CampaignStatusCd.COMPLETED;
    }

    public boolean canStart() {
        return this.status == CampaignStatusCd.READY
                && LocalDateTime.now().isAfter(this.scheduledAt);
    }

    private void validateCampaignKey(String campaignKey) {
        if (campaignKey == null || campaignKey.trim().isEmpty()) {
            throw new CampaignValidationException(ErrorCode.CAMPAIGN_KEY_REQUIRED);
        }
        if (campaignKey.length() > KEY_MAX_LENGTH) {
            throw new CampaignValidationException(ErrorCode.CAMPAIGN_KEY_TOO_LONG);
        }
    }

    private void validateScheduledAt(LocalDateTime scheduledAt) {
        if (scheduledAt == null) {
            throw new CampaignValidationException(ErrorCode.CAMPAIGN_SCHEDULED_AT_REQUIRED);
        }
    }
}
