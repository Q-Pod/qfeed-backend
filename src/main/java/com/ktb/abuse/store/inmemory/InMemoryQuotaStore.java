package com.ktb.abuse.store.inmemory;

import com.ktb.abuse.store.QuotaStore;
import com.ktb.abuse.value.DailyQuota;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!redis")
public class InMemoryQuotaStore implements QuotaStore {

    private final ConcurrentHashMap<String, DailyQuota> quotas = new ConcurrentHashMap<>();

    @Override
    public int incrementDailyQuota(Long accountId) {
        String key = buildKey(accountId);
        LocalDate today = LocalDate.now();

        DailyQuota quota = quotas.compute(key, (k, existing) -> {
            if (existing == null || !existing.date().equals(today)) {
                return new DailyQuota(today, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });

        return quota.count().get();
    }

    @Override
    public int getDailyQuota(Long accountId) {
        String key = buildKey(accountId);
        LocalDate today = LocalDate.now();

        DailyQuota quota = quotas.get(key);
        if (quota == null || !quota.date().equals(today)) {
            return 0;
        }
        return quota.count().get();
    }

    private String buildKey(Long accountId) {
        return "quota:" + accountId;
    }
}
