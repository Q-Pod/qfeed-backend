package com.ktb.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoticeCreateRequest(
        @NotBlank(message = "공지 제목은 필수입니다")
        @Size(max = 200, message = "공지 제목은 200자를 초과할 수 없습니다")
        String title,

        @NotBlank(message = "공지 내용은 필수입니다")
        @Size(max = 2000, message = "공지 내용은 2000자를 초과할 수 없습니다")
        String body,

        @Size(max = 500, message = "딥링크는 500자를 초과할 수 없습니다")
        String deeplink
) {
}
