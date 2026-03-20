package com.ktb.abuse.store.redis;

import com.ktb.abuse.store.ContentHashStore;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Primary
@Profile("redis")
public class RedisContentHashStore implements ContentHashStore {

    private static final String EXACT_KEY_PREFIX = "abuse:hash:exact:";
    private static final String SIM_KEY_PREFIX = "abuse:hash:sim:";
    private static final long HASH_TTL_SECONDS = 7L * 24 * 60 * 60;
    private static final long SIM_HASH_MAX_SIZE = 100L;

    private final StringRedisTemplate redisTemplate;

    @Override
    public void storeHash(Long accountId, Long questionId, String hash, String simHash) {
        String exactKey = exactKey(accountId, questionId);
        String simKey = simKey(accountId, questionId);

        redisTemplate.opsForSet().add(exactKey, hash);
        redisTemplate.expire(exactKey, HASH_TTL_SECONDS, TimeUnit.SECONDS);

        redisTemplate.opsForList().leftPush(simKey, simHash);
        redisTemplate.opsForList().trim(simKey, 0, SIM_HASH_MAX_SIZE - 1);
        redisTemplate.expire(simKey, HASH_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public boolean existsHash(Long accountId, Long questionId, String hash) {
        Boolean result = redisTemplate.opsForSet().isMember(exactKey(accountId, questionId), hash);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public List<String> getRecentSimHashes(Long accountId, Long questionId, int limit) {
        List<String> result = redisTemplate.opsForList().range(simKey(accountId, questionId), 0, limit - 1);
        return result != null ? result : Collections.emptyList();
    }

    private String exactKey(Long accountId, Long questionId) {
        return EXACT_KEY_PREFIX + accountId + ":" + questionId;
    }

    private String simKey(Long accountId, Long questionId) {
        return SIM_KEY_PREFIX + accountId + ":" + questionId;
    }
}
