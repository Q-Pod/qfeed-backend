package com.ktb.notification.dto.request;

import com.ktb.notification.domain.enums.TargetUsersType;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record NoticePublishRequest(
    OffsetDateTime scheduledAt,
    @NotNull TargetUsersType targetUsers
) {}
