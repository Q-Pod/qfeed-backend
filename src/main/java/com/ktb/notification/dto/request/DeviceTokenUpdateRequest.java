package com.ktb.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeviceTokenUpdateRequest(
        @NotBlank(message = "푸시 토큰은 필수입니다")
        String pushToken
) {
}
