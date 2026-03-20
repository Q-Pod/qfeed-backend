package com.ktb.abuse.store.redis;

import com.ktb.abuse.store.CooldownStore;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
public class RedisCooldownStore implements CooldownStore {

    private static final String LAST_KEY_PREFIX = "abuse:cooldown:last:";
    private static final String CONSEC_KEY_PREFIX = "abuse:cooldown:consec:";
    private static final long COOLDOWN_TTL_SECONDS = 86400L;

    private static final RedisScript<Long> INCR_WITH_EXPIRE = RedisScript.of(
        "local v = redis.call('INCR', KEYS[1])\n"
        + "if v == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end\n"
        + "return v",
        Long.class
    );

    private final StringRedisTemplate redisTemplate;

    @Override
    public void setLastSubmitTime(Long accountId, Long questionId, long timestamp) {
        redisTemplate.opsForValue().set(
            lastKey(accountId, questionId),
            String.valueOf(timestamp),
            COOLDOWN_TTL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    @Override
    public Optional<Long> getLastSubmitTime(Long accountId, Long questionId) {
        String value = redisTemplate.opsForValue().get(lastKey(accountId, questionId));
        return Optional.ofNullable(value).map(Long::parseLong);
    }

    @Override
    public void incrementConsecutiveCount(Long accountId, Long questionId) {
        redisTemplate.execute(
            INCR_WITH_EXPIRE,
            List.of(consecKey(accountId, questionId)),
            String.valueOf(COOLDOWN_TTL_SECONDS)
        );
    }

    @Override
    public int getConsecutiveCount(Long accountId, Long questionId) {
        String value = redisTemplate.opsForValue().get(consecKey(accountId, questionId));
        return value != null ? Integer.parseInt(value) : 0;
    }

    @Override
    public void resetConsecutiveCount(Long accountId, Long questionId) {
        redisTemplate.delete(consecKey(accountId, questionId));
    }

    private String lastKey(Long accountId, Long questionId) {
        return LAST_KEY_PREFIX + accountId + ":" + questionId;
    }

    private String consecKey(Long accountId, Long questionId) {
        return CONSEC_KEY_PREFIX + accountId + ":" + questionId;
    }
}
