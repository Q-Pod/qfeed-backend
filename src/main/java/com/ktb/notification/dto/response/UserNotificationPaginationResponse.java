package com.ktb.notification.dto.response;

public record UserNotificationPaginationResponse(
        Long nextCursor,
        boolean hasNext,
        int size
) {
}
