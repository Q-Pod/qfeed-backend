package com.ktb.notification.dto.request;

import com.ktb.notification.domain.enums.DevicePlatformCd;
import jakarta.validation.constraints.NotNull;

public record DeviceRegisterRequest(
        @NotNull(message = "플랫폼은 필수입니다")
        DevicePlatformCd platform
) {
}
