package com.ktb.auth.service.impl;

import com.ktb.auth.dto.jwt.RefreshTokenInfo;
import com.ktb.auth.service.RefreshTokenStore;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

/**
 * Redis 기반 Refresh Token 저장소 구현체 (Cache-Aside 패턴)
 * DB가 Source of Truth, Redis는 읽기 캐시
 * <p>
 * Key 설계:
 * - auth:refresh:hash:{tokenHash} → "{id}|{familyId}|{used}|{expiresAtIso}"
 * - auth:refresh:id:{tokenId}     → tokenHash (역방향 인덱스, markAsUsed 시 무효화용)
 */
@Slf4j
@Service
@Primary
@Profile("redis")
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String HASH_KEY_PREFIX = "auth:refresh:hash:";
    private static final String ID_KEY_PREFIX = "auth:refresh:id:";
    private static final String FIELD_DELIMITER = "|";

    private static final RedisScript<Void> SET_WITH_EXPIRE_PAIR = RedisScript.of(
        "redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[3])\n"
        + "redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[3])\n"
        + "return nil",
        Void.class
    );

    private final JpaRefreshTokenStore jpaStore;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(Long familyId, String tokenHash, LocalDateTime expiresAt) {
        jpaStore.save(familyId, tokenHash, expiresAt);

        jpaStore.findByTokenHash(tokenHash).ifPresent(info -> {
            long ttlSeconds = computeTtlSeconds(info.expiresAt());
            if (ttlSeconds <= 0) {
                return;
            }
            String hashKey = HASH_KEY_PREFIX + tokenHash;
            String idKey = ID_KEY_PREFIX + info.id();
            String value = serialize(info);

            redisTemplate.execute(
                SET_WITH_EXPIRE_PAIR,
                List.of(hashKey, idKey),
                value, tokenHash, String.valueOf(ttlSeconds)
            );
            log.debug("RefreshToken 캐시 PUT: tokenId={}", info.id());
        });
    }

    @Override
    public Optional<RefreshTokenInfo> findByTokenHash(String tokenHash) {
        String cached = redisTemplate.opsForValue().get(HASH_KEY_PREFIX + tokenHash);
        if (cached != null) {
            log.debug("RefreshToken 캐시 HIT: tokenHash={}...", tokenHash.substring(0, 8));
            return Optional.of(deserialize(cached, tokenHash));
        }

        log.debug("RefreshToken 캐시 MISS: tokenHash={}...", tokenHash.substring(0, 8));
        Optional<RefreshTokenInfo> result = jpaStore.findByTokenHash(tokenHash);
        result.ifPresent(info -> {
            long ttlSeconds = computeTtlSeconds(info.expiresAt());
            if (ttlSeconds <= 0) {
                return;
            }
            redisTemplate.opsForValue().set(
                HASH_KEY_PREFIX + tokenHash, serialize(info), ttlSeconds, TimeUnit.SECONDS
            );
            redisTemplate.opsForValue().set(
                ID_KEY_PREFIX + info.id(), tokenHash, ttlSeconds, TimeUnit.SECONDS
            );
        });
        return result;
    }

    @Override
    public void markAsUsed(Long tokenId) {
        jpaStore.markAsUsed(tokenId);

        String tokenHash = redisTemplate.opsForValue().get(ID_KEY_PREFIX + tokenId);
        if (tokenHash != null) {
            redisTemplate.delete(HASH_KEY_PREFIX + tokenHash);
            redisTemplate.delete(ID_KEY_PREFIX + tokenId);
            log.debug("RefreshToken 캐시 무효화: tokenId={}", tokenId);
        }
    }

    private String serialize(RefreshTokenInfo info) {
        return info.id() + FIELD_DELIMITER
            + info.familyId() + FIELD_DELIMITER
            + info.used() + FIELD_DELIMITER
            + info.expiresAt().toString();
    }

    private RefreshTokenInfo deserialize(String value, String tokenHash) {
        String[] parts = value.split("\\" + FIELD_DELIMITER, 4);
        return new RefreshTokenInfo(
            Long.parseLong(parts[0]),
            Long.parseLong(parts[1]),
            Boolean.parseBoolean(parts[2]),
            LocalDateTime.parse(parts[3]),
            tokenHash
        );
    }

    private long computeTtlSeconds(LocalDateTime expiresAt) {
        return Duration.between(LocalDateTime.now(), expiresAt).toSeconds();
    }
}
