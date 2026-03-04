package com.ktb.async.contract;

import java.time.Instant;

public interface BaseEvent {

    String eventId();

    String eventType();

    Instant eventTime();

    String source();

    String traceId();

    Integer retryCount();
}
