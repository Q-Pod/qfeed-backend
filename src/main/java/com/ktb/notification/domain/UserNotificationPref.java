package com.ktb.notification.domain;

import com.ktb.auth.domain.UserAccount;
import com.ktb.common.domain.BaseTimeEntity;
import com.ktb.notification.domain.enums.NotificationTypeCd;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "USER_NOTIFICATION_PREF",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_account_noti_type",
                        columnNames = {"account_id", "noti_type_cd"}
                )
        },
        indexes = {
                @Index(name = "idx_account_id", columnList = "account_id"),
                @Index(name = "idx_noti_type", columnList = "noti_type_cd")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotificationPref extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_noti_pref_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private UserAccount account;

    @Enumerated(EnumType.STRING)
    @Column(name = "noti_type_cd", nullable = false, length = 50)
    private NotificationTypeCd notificationType;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Builder
    private UserNotificationPref(UserAccount account, NotificationTypeCd notificationType, boolean enabled) {
        this.account = account;
        this.notificationType = notificationType;
        this.enabled = enabled;
    }

    public static UserNotificationPref create(UserAccount account, NotificationTypeCd notificationType) {
        return UserNotificationPref.builder()
                .account(account)
                .notificationType(notificationType)
                .enabled(true)
                .build();
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
