package com.ktb.abuse.store;

import java.time.Duration;
import java.util.Optional;

public interface RateLimitStore {

    long incrementAndGet(String key, Duration window);

    Optional<Long> get(String key);
}
