package com.ktb.abuse.store.redis;

import com.ktb.abuse.store.RateLimitStore;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
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
public class RedisRateLimitStore implements RateLimitStore {

    private static final String KEY_PREFIX = "abuse:rate:";

    private static final RedisScript<Long> INCREMENT_WITH_EXPIRE = RedisScript.of(
        "local v = redis.call('INCR', KEYS[1])\n"
        + "if v == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end\n"
        + "return v",
        Long.class
    );

    private final StringRedisTemplate redisTemplate;

    @Override
    public long incrementAndGet(String key, Duration window) {
        String redisKey = KEY_PREFIX + key;
        Long count = redisTemplate.execute(
            INCREMENT_WITH_EXPIRE,
            List.of(redisKey),
            String.valueOf(window.toMillis())
        );
        return count != null ? count : 1L;
    }

    @Override
    public Optional<Long> get(String key) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + key);
        return Optional.ofNullable(value).map(Long::parseLong);
    }
}
