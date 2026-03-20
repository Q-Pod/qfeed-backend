package com.ktb.abuse.store.redis;

import com.ktb.abuse.store.QuotaStore;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Primary
@Profile("redis")
public class RedisQuotaStore implements QuotaStore {

    private static final String KEY_PREFIX = "abuse:quota:";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long QUOTA_TTL_SECONDS = 48L * 60 * 60;

    private static final RedisScript<Long> INCR_WITH_EXPIRE = RedisScript.of(
        "local v = redis.call('INCR', KEYS[1])\n"
        + "if v == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end\n"
        + "return v",
        Long.class
    );

    private final StringRedisTemplate redisTemplate;

    @Override
    public int incrementDailyQuota(Long accountId) {
        String key = dailyKey(accountId);
        Long count = redisTemplate.execute(
            INCR_WITH_EXPIRE,
            List.of(key),
            String.valueOf(QUOTA_TTL_SECONDS)
        );
        return count != null ? count.intValue() : 1;
    }

    @Override
    public int getDailyQuota(Long accountId) {
        String value = redisTemplate.opsForValue().get(dailyKey(accountId));
        return value != null ? Integer.parseInt(value) : 0;
    }

    private String dailyKey(Long accountId) {
        return KEY_PREFIX + accountId + ":" + LocalDate.now().format(DATE_FORMAT);
    }
}
