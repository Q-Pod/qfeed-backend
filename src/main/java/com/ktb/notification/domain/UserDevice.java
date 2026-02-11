package com.ktb.notification.domain;

import com.ktb.auth.domain.UserAccount;
import com.ktb.common.domain.BaseTimeEntity;
import com.ktb.common.domain.ErrorCode;
import com.ktb.notification.domain.enums.DevicePlatformCd;
import com.ktb.notification.domain.enums.DevicePushTypeCd;
import com.ktb.notification.exception.UserDeviceValidationException;
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
        name = "USER_DEVICE",
        indexes = {
                @Index(name = "idx_device_account", columnList = "account_id, is_active"),
                @Index(name = "idx_device_token", columnList = "device_push_token"),
                @Index(name = "idx_device_platform", columnList = "device_platform_cd, is_active")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDevice extends BaseTimeEntity {

    private static final int PUSH_TOKEN_MAX_LENGTH = 500;
    private static final int PUSH_ENDPOINT_MAX_LENGTH = 2000;
    private static final int PUSH_KEY_MAX_LENGTH = 256;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "device_push_type_cd", nullable = false, length = 20)
    private DevicePushTypeCd pushType;

    @Column(name = "device_push_token", length = 500)
    private String pushToken;

    @Column(name = "device_push_endpoint", length = 2000)
    private String pushEndpoint;

    @Column(name = "device_push_p256dh", length = 256)
    private String pushP256dh;

    @Column(name = "device_push_auth", length = 256)
    private String pushAuth;

    @Column(name = "device_push_expired_at")
    private LocalDateTime pushExpiredAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "token_updated_at")
    private LocalDateTime tokenUpdatedAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Builder
    private UserDevice(
            UserAccount account,
            DevicePlatformCd platform,
            DevicePushTypeCd pushType,
            String pushToken,
            String pushEndpoint,
            String pushP256dh,
            String pushAuth,
            LocalDateTime pushExpiredAt
    ) {
        validatePushCredentials(pushType, pushToken, pushEndpoint);
        this.account = account;
        this.platform = platform;
        this.pushType = pushType;
        this.pushToken = pushToken;
        this.pushEndpoint = pushEndpoint;
        this.pushP256dh = pushP256dh;
        this.pushAuth = pushAuth;
        this.pushExpiredAt = pushExpiredAt;
        this.active = true;
        this.tokenUpdatedAt = LocalDateTime.now();
        this.lastSeenAt = LocalDateTime.now();
    }

    public static UserDevice createMobileDevice(
            UserAccount account,
            DevicePlatformCd platform,
            DevicePushTypeCd pushType,
            String pushToken
    ) {
        return UserDevice.builder()
                .account(account)
                .platform(platform)
                .pushType(pushType)
                .pushToken(pushToken)
                .build();
    }

    public static UserDevice createWebPushDevice(
            UserAccount account,
            String pushEndpoint,
            String pushP256dh,
            String pushAuth,
            LocalDateTime pushExpiredAt
    ) {
        return UserDevice.builder()
                .account(account)
                .platform(DevicePlatformCd.WEB)
                .pushType(DevicePushTypeCd.WEBPUSH)
                .pushEndpoint(pushEndpoint)
                .pushP256dh(pushP256dh)
                .pushAuth(pushAuth)
                .pushExpiredAt(pushExpiredAt)
                .build();
    }

    public void updatePushToken(String newToken) {
        if (newToken == null || newToken.trim().isEmpty()) {
            throw new UserDeviceValidationException(ErrorCode.DEVICE_TOKEN_REQUIRED);
        }
        this.pushToken = newToken;
        this.tokenUpdatedAt = LocalDateTime.now();
    }

    public void updateWebPushCredentials(
            String pushEndpoint,
            String pushP256dh,
            String pushAuth,
            LocalDateTime pushExpiredAt
    ) {
        this.pushEndpoint = pushEndpoint;
        this.pushP256dh = pushP256dh;
        this.pushAuth = pushAuth;
        this.pushExpiredAt = pushExpiredAt;
        this.tokenUpdatedAt = LocalDateTime.now();
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

    public boolean isWebPush() {
        return this.pushType == DevicePushTypeCd.WEBPUSH;
    }

    public boolean isMobilePush() {
        return this.pushType == DevicePushTypeCd.APNS_TOKEN
                || this.pushType == DevicePushTypeCd.FCM_TOKEN;
    }

    public boolean isExpired() {
        if (this.pushExpiredAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(this.pushExpiredAt);
    }

    private void validatePushCredentials(
            DevicePushTypeCd pushType,
            String pushToken,
            String pushEndpoint
    ) {
        if (pushType == DevicePushTypeCd.WEBPUSH) {
            if (pushEndpoint == null || pushEndpoint.trim().isEmpty()) {
                throw new UserDeviceValidationException(ErrorCode.DEVICE_TOKEN_REQUIRED);
            }
        } else {
            if (pushToken == null || pushToken.trim().isEmpty()) {
                throw new UserDeviceValidationException(ErrorCode.DEVICE_TOKEN_REQUIRED);
            }
        }
    }
}
