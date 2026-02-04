package com.ktb.abuse.guard.ratelimit;

import com.ktb.abuse.config.AbuseGuardProperties;
import com.ktb.abuse.core.AbuseCheckContext;
import com.ktb.abuse.core.AbuseGuardResult;
import com.ktb.abuse.guard.Guard;
import com.ktb.abuse.store.CooldownStore;
import com.ktb.abuse.store.RateLimitStore;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "abuse.guard.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class InMemoryRateLimitGuard implements Guard {

    private static final String GUARD_NAME = "RateLimitGuard";
    private static final int ORDER = 1;

    private static final String KEY_USER_MINUTE = "user:minute:";
    private static final String KEY_USER_HOUR = "user:hour:";
    private static final String KEY_IP_MINUTE = "ip:minute:";

    private final RateLimitStore rateLimitStore;
    private final CooldownStore cooldownStore;
    private final AbuseGuardProperties properties;

    @Override
    public AbuseGuardResult check(AbuseCheckContext context) {
        AbuseGuardResult cooldownResult = checkQuestionCooldown(context);
        if (cooldownResult.isRejected()) {
            return cooldownResult;
        }

        AbuseGuardResult consecutiveResult = checkConsecutiveSubmit(context);
        if (consecutiveResult.isRejected()) {
            return consecutiveResult;
        }

        AbuseGuardResult userMinuteResult = checkUserPerMinute(context);
        if (userMinuteResult.isRejected()) {
            return userMinuteResult;
        }

        AbuseGuardResult userHourResult = checkUserPerHour(context);
        if (userHourResult.isRejected()) {
            return userHourResult;
        }

        AbuseGuardResult ipResult = checkIpPerMinute(context);
        if (ipResult.isRejected()) {
            return ipResult;
        }

        updateAfterCheck(context);

        return AbuseGuardResult.accept();
    }

    private AbuseGuardResult checkQuestionCooldown(AbuseCheckContext context) {
        var lastSubmitTime = cooldownStore.getLastSubmitTime(context.getAccountId(), context.getQuestionId());

        if (lastSubmitTime.isPresent()) {
            long elapsedSeconds = (System.currentTimeMillis() - lastSubmitTime.get()) / 1000;
            int cooldownSeconds = properties.getQuestionCooldownSeconds();

            if (elapsedSeconds < cooldownSeconds) {
                long remainingSeconds = cooldownSeconds - elapsedSeconds;
                log.warn("Question cooldown active - accountId: {}, questionId: {}, remaining: {}s",
                        context.getAccountId(), context.getQuestionId(), remainingSeconds);
                return AbuseGuardResult.reject(GUARD_NAME,
                        String.format("동일 질문에 대한 재시도 대기 시간이 %d초 남았습니다", remainingSeconds));
            }
        }
        return AbuseGuardResult.accept();
    }

    private AbuseGuardResult checkConsecutiveSubmit(AbuseCheckContext context) {
        int consecutiveCount = cooldownStore.getConsecutiveCount(context.getAccountId(), context.getQuestionId());
        int limit = properties.getQuestionConsecutiveLimit();

        if (consecutiveCount >= limit) {
            log.warn("Consecutive submit limit exceeded - accountId: {}, questionId: {}, count: {}",
                    context.getAccountId(), context.getQuestionId(), consecutiveCount);
            return AbuseGuardResult.reject(GUARD_NAME,
                    String.format("동일 질문에 대한 연속 제출 횟수(%d회)를 초과했습니다", limit));
        }
        return AbuseGuardResult.accept();
    }

    private AbuseGuardResult checkUserPerMinute(AbuseCheckContext context) {
        String key = KEY_USER_MINUTE + context.getAccountId();
        long count = rateLimitStore.incrementAndGet(key, Duration.ofMinutes(1));
        int limit = properties.getUserPerMinute();

        if (count > limit) {
            log.warn("User per-minute rate limit exceeded - accountId: {}, count: {}, limit: {}",
                    context.getAccountId(), count, limit);
            return AbuseGuardResult.reject(GUARD_NAME,
                    String.format("분당 제출 횟수(%d회)를 초과했습니다", limit));
        }
        return AbuseGuardResult.accept();
    }

    private AbuseGuardResult checkUserPerHour(AbuseCheckContext context) {
        String key = KEY_USER_HOUR + context.getAccountId();
        long count = rateLimitStore.incrementAndGet(key, Duration.ofHours(1));
        int limit = properties.getUserPerHour();

        if (count > limit) {
            log.warn("User per-hour rate limit exceeded - accountId: {}, count: {}, limit: {}",
                    context.getAccountId(), count, limit);
            return AbuseGuardResult.reject(GUARD_NAME,
                    String.format("시간당 제출 횟수(%d회)를 초과했습니다", limit));
        }
        return AbuseGuardResult.accept();
    }

    private AbuseGuardResult checkIpPerMinute(AbuseCheckContext context) {
        if (context.getClientIp() == null || context.getClientIp().isBlank()) {
            return AbuseGuardResult.accept();
        }

        String key = KEY_IP_MINUTE + context.getClientIp();
        long count = rateLimitStore.incrementAndGet(key, Duration.ofMinutes(1));
        int limit = properties.getIpPerMinute();

        if (count > limit) {
            log.warn("IP per-minute rate limit exceeded - ip: {}, count: {}, limit: {}",
                    context.getClientIp(), count, limit);
            return AbuseGuardResult.reject(GUARD_NAME,
                    String.format("IP당 분당 제출 횟수(%d회)를 초과했습니다", limit));
        }
        return AbuseGuardResult.accept();
    }

    private void updateAfterCheck(AbuseCheckContext context) {
        cooldownStore.setLastSubmitTime(context.getAccountId(), context.getQuestionId(), System.currentTimeMillis());
        cooldownStore.incrementConsecutiveCount(context.getAccountId(), context.getQuestionId());
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
