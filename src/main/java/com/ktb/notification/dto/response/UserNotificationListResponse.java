package com.ktb.notification.dto.response;

import java.util.List;

public record UserNotificationListResponse(
        List<UserNotificationResponse> notifications,
        UserNotificationPaginationResponse pagination
) {
}
