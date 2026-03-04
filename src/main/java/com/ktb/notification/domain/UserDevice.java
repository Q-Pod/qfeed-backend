package com.ktb.notification.domain;

import com.ktb.auth.domain.UserAccount;
import com.ktb.common.domain.BaseTimeEntity;
import com.ktb.notification.domain.enums.DevicePlatformCd;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "USER_DEVICE",
        indexes = {
                @Index(name = "idx_device_account", columnList = "account_id, is_active"),
                @Index(name = "idx_device_platform", columnList = "device_platform_cd, is_active")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDevice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private UserAccount account;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_platform_cd", nullable = false, length = 20)
    private DevicePlatformCd platform;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    public static UserDevice create(UserAccount account, DevicePlatformCd platform) {
        UserDevice device = new UserDevice();
        device.account = account;
        device.platform = platform;
        device.active = true;
        device.lastSeenAt = LocalDateTime.now();
        return device;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void updateLastSeen() {
        this.lastSeenAt = LocalDateTime.now();
    }
}
