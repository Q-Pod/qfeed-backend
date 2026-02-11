package com.ktb.notification.dto.request;

import jakarta.validation.constraints.Size;

public record NoticeUpdateRequest(
        @Size(max = 200, message = "공지 제목은 200자를 초과할 수 없습니다")
        String title,

        @Size(max = 2000, message = "공지 내용은 2000자를 초과할 수 없습니다")
        String body,

        @Size(max = 500, message = "딥링크는 500자를 초과할 수 없습니다")
        String deeplink
) {
}
