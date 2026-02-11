package com.ktb.notification.dto.request;

import jakarta.validation.constraints.NotNull;

public record NotificationPrefUpdateRequest(
        @NotNull(message = "수신 여부는 필수입니다")
        Boolean enabled
) {
}
