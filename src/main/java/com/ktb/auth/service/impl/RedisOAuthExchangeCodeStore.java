package com.ktb.auth.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.auth.dto.OAuthExchangePayload;
import com.ktb.auth.exception.oauth.InvalidExchangeCodeException;
import com.ktb.auth.service.OAuthExchangeCodeStore;
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
public class RedisOAuthExchangeCodeStore implements OAuthExchangeCodeStore {

    private static final String PREFIX = "oauth:exchange:";
    private static final Duration TTL = Duration.ofMinutes(1);

    private static final RedisScript<String> GET_AND_DELETE_SCRIPT = RedisScript.of(
            "local v = redis.call('GET', KEYS[1])\n" +
                    "if v then redis.call('DEL', KEYS[1]) return v else return nil end",
            String.class
    );

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String generateAndStore(OAuthExchangePayload payload) {
        String code = UUID.randomUUID().toString();
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(PREFIX + code, json, TTL);
            log.info("Exchange code 생성 (Redis): accountId={}, code={}", payload.accountId(), code);
            return code;
        } catch (JsonProcessingException e) {
            log.error("ExchangeCode 직렬화 실패: {}", e.getMessage());
            throw new IllegalStateException("ExchangeCode 저장 실패", e);
        }
    }

    @Override
    public OAuthExchangePayload validateAndConsume(String exchangeCode) {
        String json = redisTemplate.execute(
                GET_AND_DELETE_SCRIPT,
                List.of(PREFIX + exchangeCode)
        );
        if (json == null) {
            throw new InvalidExchangeCodeException();
        }
        try {
            return objectMapper.readValue(json, OAuthExchangePayload.class);
        } catch (JsonProcessingException e) {
            log.error("ExchangeCode 역직렬화 실패: {}", e.getMessage());
            throw new InvalidExchangeCodeException();
        }
    }
}