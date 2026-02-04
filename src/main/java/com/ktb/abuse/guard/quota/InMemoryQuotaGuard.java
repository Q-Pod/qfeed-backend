package com.ktb.abuse.guard.quota;

import com.ktb.abuse.config.AbuseGuardProperties;
import com.ktb.abuse.core.AbuseCheckContext;
import com.ktb.abuse.core.AbuseGuardResult;
import com.ktb.abuse.guard.Guard;
import com.ktb.abuse.store.QuotaStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "abuse.guard.quota", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class InMemoryQuotaGuard implements Guard {

    private static final String GUARD_NAME = "QuotaGuard";
    private static final int ORDER = 5;

    private final QuotaStore quotaStore;
    private final AbuseGuardProperties properties;

    @Override
    public AbuseGuardResult check(AbuseCheckContext context) {
        int currentQuota = quotaStore.getDailyQuota(context.getAccountId());
        int dailyLimit = properties.getDailyQuotaLimit();
        int softLimit = properties.getSoftQuotaLimit();

        if (currentQuota >= dailyLimit) {
            log.warn("Daily quota exceeded - accountId: {}, current: {}, limit: {}",
                    context.getAccountId(), currentQuota, dailyLimit);
            return AbuseGuardResult.reject(GUARD_NAME,
                    String.format("일일 답변 제출 한도(%d회)를 초과했습니다", dailyLimit));
        }

        int newQuota = quotaStore.incrementDailyQuota(context.getAccountId());

        if (newQuota > softLimit) {
            log.info("Soft quota exceeded - accountId: {}, current: {}, softLimit: {}",
                    context.getAccountId(), newQuota, softLimit);
            return AbuseGuardResult.acceptNoFeedback(GUARD_NAME,
                    String.format("일일 피드백 제공 한도(%d회)를 초과하여 저장만 됩니다", softLimit),
                    50);
        }

        return AbuseGuardResult.accept();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public String getName() {
        return GUARD_NAME;
    }
}
