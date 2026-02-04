package com.ktb.abuse.value;

import java.util.concurrent.atomic.AtomicLong;

public record WindowedCounter(long startTime, AtomicLong count) {
    public WindowedCounter(long startTime, long count) {
        this(startTime, new AtomicLong(count));
    }
}
