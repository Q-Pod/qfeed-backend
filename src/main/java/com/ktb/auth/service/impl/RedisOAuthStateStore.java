package com.ktb.auth.service.impl;

import com.ktb.auth.exception.oauth.InvalidStateException;
import com.ktb.auth.service.OAuthStateStore;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
@Profile("redis")
@RequiredArgsConstructor
public class RedisOAuthStateStore implements OAuthStateStore {

    private static final String PREFIX = "oauth:state:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private static final RedisScript<Long> GET_AND_DELETE_SCRIPT = RedisScript.of(
            "local v = redis.call('GET', KEYS[1])\n" +
                    "if v then redis.call('DEL', KEYS[1]) return 1 else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    @Override
    public String generateAndStore(String provider) {
        String state = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PREFIX + state, provider, TTL);
        log.info("State 생성 (Redis): provider={}, state={}", provider, state);
        return state;
    }

    @Override
    public void validateAndConsume(String state) {
        Long result = redisTemplate.execute(
                GET_AND_DELETE_SCRIPT,
                List.of(PREFIX + state)
        );
        if (result == null || result == 0L) {
            throw new InvalidStateException();
        }
    }
}