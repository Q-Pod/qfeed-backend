package com.ktb.notification.event;

import com.ktb.notification.domain.enums.TargetUsersType;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NoticePublishedEvent extends ApplicationEvent {

    private final Long noticeId;
    private final Long campaignId;
    private final TargetUsersType targetUsers;
    private final LocalDateTime scheduledAt;

    public NoticePublishedEvent(
            Object source,
            Long noticeId,
            Long campaignId,
            TargetUsersType targetUsers,
            LocalDateTime scheduledAt
    ) {
        super(source);
        this.noticeId = noticeId;
        this.campaignId = campaignId;
        this.targetUsers = targetUsers;
        this.scheduledAt = scheduledAt;
    }
}
