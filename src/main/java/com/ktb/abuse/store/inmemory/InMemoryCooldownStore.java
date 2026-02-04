package com.ktb.abuse.store.inmemory;

import com.ktb.abuse.store.CooldownStore;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!redis")
public class InMemoryCooldownStore implements CooldownStore {

    private final ConcurrentHashMap<String, Long> lastSubmitTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> consecutiveCounts = new ConcurrentHashMap<>();

    @Override
    public void setLastSubmitTime(Long accountId, Long questionId, long timestamp) {
        String key = buildKey(accountId, questionId);
        lastSubmitTimes.put(key, timestamp);
    }

    @Override
    public Optional<Long> getLastSubmitTime(Long accountId, Long questionId) {
        String key = buildKey(accountId, questionId);
        return Optional.ofNullable(lastSubmitTimes.get(key));
    }

    @Override
    public void incrementConsecutiveCount(Long accountId, Long questionId) {
        String key = buildKey(accountId, questionId);
        consecutiveCounts.computeIfAbsent(key, k -> new AtomicInteger(0))
            .incrementAndGet();
    }

    @Override
    public int getConsecutiveCount(Long accountId, Long questionId) {
        String key = buildKey(accountId, questionId);
        AtomicInteger count = consecutiveCounts.get(key);
        return ((count == null) ? 0 : count.get());
    }

    @Override
    public void resetConsecutiveCount(Long accountId, Long questionId) {
        String key = buildKey(accountId, questionId);
        consecutiveCounts.remove(key);
    }

    private String buildKey(Long accountId, Long questionId) {
        return accountId + ":" + questionId;
    }
}
