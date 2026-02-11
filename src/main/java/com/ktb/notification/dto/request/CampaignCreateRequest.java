package com.ktb.notification.dto.request;

import com.ktb.notification.domain.enums.NotificationTypeCd;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CampaignCreateRequest(
        @NotNull(message = "캠페인 유형은 필수입니다")
        NotificationTypeCd campaignType,

        @NotBlank(message = "캠페인 키는 필수입니다")
        @Size(max = 200, message = "캠페인 키는 200자를 초과할 수 없습니다")
        String campaignKey,

        @NotNull(message = "예약 시간은 필수입니다")
        @Future(message = "예약 시간은 현재 시간 이후여야 합니다")
        LocalDateTime scheduledAt
) {
}
