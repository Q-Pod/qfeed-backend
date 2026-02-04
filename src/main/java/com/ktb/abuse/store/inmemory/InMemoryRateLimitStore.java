package com.ktb.abuse.store.inmemory;

import com.ktb.abuse.store.RateLimitStore;
import com.ktb.abuse.value.WindowedCounter;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!redis")
public class InMemoryRateLimitStore implements RateLimitStore {

    private final ConcurrentHashMap<String, WindowedCounter> counters = new ConcurrentHashMap<>();

    @Override
    public long incrementAndGet(String key, Duration window) {
        long windowMillis = window.toMillis();
        WindowedCounter counter = counters.compute(key, (k, existing) -> {
            long now = Instant.now().toEpochMilli();
            if (existing == null || isExpired(existing, now, windowMillis)) {
                return new WindowedCounter(now, 1L);
            }
            return new WindowedCounter(existing.startTime(), existing.count().incrementAndGet());
        });
        return counter.count().get();
    }

    @Override
    public Optional<Long> get(String key) {
        WindowedCounter counter = counters.get(key);
        if (counter == null) {
            return Optional.empty();
        }
        return Optional.of(counter.count().get());
    }

    private boolean isExpired(WindowedCounter counter, long now, long windowMillis) {
        return now - counter.startTime() >= windowMillis;
    }
}
