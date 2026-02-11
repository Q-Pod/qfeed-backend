package com.ktb.notification.dto.response;

import com.ktb.notification.domain.Notice;
import com.ktb.notification.domain.enums.NoticeStatusCd;
import java.time.LocalDateTime;

public record NoticeResponse(
        Long id,
        String title,
        String body,
        String deeplink,
        NoticeStatusCd status,
        String statusDescription,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
) {

    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getBody(),
                notice.getDeeplink(),
                notice.getStatus(),
                notice.getStatus().getDescription(),
                notice.getPublishedAt(),
                notice.getCreatedAt()
        );
    }
}
