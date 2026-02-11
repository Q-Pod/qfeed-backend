package com.ktb.notification.domain;

import com.ktb.common.domain.BaseSoftDeleteEntity;
import com.ktb.notification.domain.enums.NoticeStatusCd;
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
import com.ktb.notification.exception.NoticeInvalidStatusTransitionException;
import com.ktb.notification.exception.NoticeValidationException;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "NOTICE",
        indexes = {
                @Index(name = "idx_notice_status", columnList = "notice_status_cd"),
                @Index(name = "idx_notice_published", columnList = "notice_published_at"),
                @Index(name = "idx_notice_created", columnList = "created_at"),
                @Index(name = "idx_notice_status_published", columnList = "notice_status_cd, notice_published_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseSoftDeleteEntity {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int BODY_MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

    @Column(name = "notice_title", nullable = false, length = 200)
    private String title;

    @Column(name = "notice_body", nullable = false, length = 2000)
    private String body;

    @Column(name = "notice_deeplink", length = 500)
    private String deeplink;

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_status_cd", nullable = false, length = 20)
    private NoticeStatusCd status = NoticeStatusCd.DRAFT;

    @Column(name = "notice_published_at")
    private LocalDateTime publishedAt;

    @Builder
    private Notice(String title, String body, String deeplink) {
        validateTitle(title);
        validateBody(body);
        this.title = title;
        this.body = body;
        this.deeplink = deeplink;
        this.status = NoticeStatusCd.DRAFT;
    }

    public static Notice create(String title, String body, String deeplink) {
        return Notice.builder()
                .title(title)
                .body(body)
                .deeplink(deeplink)
                .build();
    }

    public void updateTitle(String title) {
        validateDraft();
        validateTitle(title);
        this.title = title;
    }

    public void updateBody(String body) {
        validateDraft();
        validateBody(body);
        this.body = body;
    }

    public void updateDeeplink(String deeplink) {
        validateDraft();
        this.deeplink = deeplink;
    }

    public void publish() {
        validateDraft();
        this.status = NoticeStatusCd.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void complete() {
        if (this.status != NoticeStatusCd.PUBLISHED) {
            throw new NoticeInvalidStatusTransitionException(this.status, NoticeStatusCd.COMPLETED);
        }
        this.status = NoticeStatusCd.COMPLETED;
    }

    public void cancel() {
        if (this.status == NoticeStatusCd.COMPLETED) {
            throw new NoticeInvalidStatusTransitionException(this.status, NoticeStatusCd.CANCELLED);
        }
        this.status = NoticeStatusCd.CANCELLED;
    }

    public void delete() {
        if (this.status == NoticeStatusCd.PUBLISHED) {
            throw new NoticeInvalidStatusTransitionException(this.status, NoticeStatusCd.DRAFT);
        }
        softDelete();
    }

    public boolean isDraft() {
        return this.status == NoticeStatusCd.DRAFT;
    }

    public boolean isPublished() {
        return this.status == NoticeStatusCd.PUBLISHED;
    }

    private void validateDraft() {
        if (this.status != NoticeStatusCd.DRAFT) {
            throw new NoticeInvalidStatusTransitionException(this.status, NoticeStatusCd.DRAFT);
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new NoticeValidationException(ErrorCode.NOTICE_TITLE_REQUIRED);
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new NoticeValidationException(ErrorCode.NOTICE_TITLE_TOO_LONG);
        }
    }

    private void validateBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            throw new NoticeValidationException(ErrorCode.NOTICE_BODY_REQUIRED);
        }
        if (body.length() > BODY_MAX_LENGTH) {
            throw new NoticeValidationException(ErrorCode.NOTICE_BODY_TOO_LONG);
        }
    }
}
