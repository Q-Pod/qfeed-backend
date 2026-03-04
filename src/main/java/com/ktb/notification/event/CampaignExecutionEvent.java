package com.ktb.notification.event;

import com.ktb.async.contract.BaseEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CampaignExecutionEvent extends ApplicationEvent implements BaseEvent {

    private static final String EVENT_TYPE = "campaign.execution";

    private final Long campaignId;
    private final String eventId;
    private final Instant eventTime;
    private final String traceId;

    public CampaignExecutionEvent(Object source, Long campaignId) {
        super(source);
        this.campaignId = campaignId;
        this.eventId = UUID.randomUUID().toString();
        this.eventTime = Instant.now();
        this.traceId = UUID.randomUUID().toString();
    }

    @Override
    public String eventId() {
        return eventId;
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public Instant eventTime() {
        return eventTime;
    }

    @Override
    public String traceId() {
        return traceId;
    }

    @Override
    public String source() {
        return "campaign-scheduler";
    }

    @Override
    public Integer retryCount() {
        return 0;
    }
}
