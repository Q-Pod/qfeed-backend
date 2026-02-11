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
import com.ktb.common.domain.ErrorCode;
import com.ktb.notification.exception.UserNotificationValidationException;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "USER_NOTIFICATION",
        indexes = {
                @Index(name = "idx_account_read", columnList = "account_id, is_read, created_at"),
                @Index(name = "idx_type_ref", columnList = "noti_type_cd, noti_reference_id"),
                @Index(name = "idx_created", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotification extends BaseTimeEntity {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int BODY_MAX_LENGTH = 500;
    private static final int DEEPLINK_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_noti_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private UserAccount account;

    @Enumerated(EnumType.STRING)
    @Column(name = "noti_type_cd", nullable = false, length = 50)
    private NotificationTypeCd notificationType;

    @Column(name = "noti_title", nullable = false, length = 200)
    private String title;

    @Column(name = "noti_body", length = 500)
    private String body;

    @Column(name = "noti_deeplink", length = 500)
    private String deeplink;

    @Column(name = "noti_reference_id")
    private Long referenceId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder
    private UserNotification(
            UserAccount account,
            NotificationTypeCd notificationType,
            String title,
            String body,
            String deeplink,
            Long referenceId
    ) {
        validateTitle(title);
        this.account = account;
        this.notificationType = notificationType;
        this.title = title;
        this.body = body;
        this.deeplink = deeplink;
        this.referenceId = referenceId;
        this.read = false;
    }

    public static UserNotification create(
            UserAccount account,
            NotificationTypeCd notificationType,
            String title,
            String body,
            String deeplink,
            Long referenceId
    ) {
        return UserNotification.builder()
                .account(account)
                .notificationType(notificationType)
                .title(title)
                .body(body)
                .deeplink(deeplink)
                .referenceId(referenceId)
                .build();
    }

    public static UserNotification createFromNotice(UserAccount account, Notice notice) {
        return UserNotification.builder()
                .account(account)
                .notificationType(NotificationTypeCd.NOTICE)
                .title(notice.getTitle())
                .body(notice.getBody())
                .deeplink(notice.getDeeplink())
                .referenceId(notice.getId())
                .build();
    }

    public void markAsRead() {
        if (!this.read) {
            this.read = true;
            this.readAt = LocalDateTime.now();
        }
    }

    public boolean isUnread() {
        return !this.read;
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new UserNotificationValidationException(ErrorCode.USER_NOTIFICATION_TITLE_REQUIRED);
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new UserNotificationValidationException(ErrorCode.USER_NOTIFICATION_TITLE_TOO_LONG);
        }
    }
}
