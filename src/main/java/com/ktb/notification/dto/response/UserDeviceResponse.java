package com.ktb.notification.dto.response;

import com.ktb.notification.domain.UserDevice;
import com.ktb.notification.domain.enums.DevicePlatformCd;
import java.time.LocalDateTime;

public record UserDeviceResponse(
        Long id,
        DevicePlatformCd platform,
        boolean active,
        LocalDateTime lastSeenAt,
        LocalDateTime createdAt
) {

    public static UserDeviceResponse from(UserDevice device) {
        return new UserDeviceResponse(
                device.getId(),
                device.getPlatform(),
                device.isActive(),
                device.getLastSeenAt(),
                device.getCreatedAt()
        );
    }
}
