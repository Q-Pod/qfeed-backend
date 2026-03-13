package com.ktb.auth.service.impl;

import com.ktb.auth.domain.RevokeReason;
import com.ktb.auth.dto.TokenFamilyInfo;
import com.ktb.auth.service.TokenFamilyStore;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

/**
 * Redis 기반 Token Family State Store
 * <p>
 * Key: auth:family:{familyUuid}  (HASH)
 * Fields:
 *   currentHash  → SHA256(현재 유효한 refresh token)
 *   revoked      → "0" | "1"
 * TTL: 갱신 성공 시마다 refresh token 만료 시간으로 리셋
 */
@Slf4j
@Service
@Primary
@Profile("redis")
@RequiredArgsConstructor
public class RedisTokenFamilyStore implements TokenFamilyStore {

    private static final String KEY_PREFIX = "auth:family:";
    private static final String FIELD_CURRENT_HASH = "currentHash";
    private static final String FIELD_REVOKED = "revoked";
    private static final String REVOKED_FALSE = "0";
    private static final String REVOKED_TRUE = "1";

    /**
     * 원자적 RTR Rotate (compare-and-swap)
     * 반환값: 1=성공, -1=key 없음, -2=revoked, -3=reuse 감지(revoke 처리됨)
     */
    private static final RedisScript<Long> ROTATE_SCRIPT = RedisScript.of(
        "local h = redis.call('HMGET', KEYS[1], 'currentHash', 'revoked')\n"
        + "local currentHash = h[1]\n"
        + "local revoked = h[2]\n"
        + "if currentHash == false then return -1 end\n"
        + "if revoked == '1' then return -2 end\n"
        + "if currentHash ~= ARGV[1] then\n"
        + "  redis.call('HSET', KEYS[1], 'revoked', '1')\n"
        + "  return -3\n"
        + "end\n"
        + "redis.call('HSET', KEYS[1], 'currentHash', ARGV[2])\n"
        + "redis.call('PEXPIRE', KEYS[1], ARGV[3])\n"
        + "return 1",
        Long.class
    );

    private final JpaTokenFamilyStore jpaStore;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<TokenFamilyInfo> findByUuid(String uuid) {
        return jpaStore.findByUuid(uuid);
    }

    @Override
    public void initFamilyState(String familyUuid, String tokenHash, long ttlMillis) {
        jpaStore.initFamilyState(familyUuid, tokenHash, ttlMillis);

        String key = familyKey(familyUuid);
        redisTemplate.opsForHash().put(key, FIELD_CURRENT_HASH, tokenHash);
        redisTemplate.opsForHash().put(key, FIELD_REVOKED, REVOKED_FALSE);
        redisTemplate.expire(key, Duration.ofMillis(ttlMillis));
        log.debug("Family 초기 상태 등록: familyUuid={}", familyUuid);
    }

    @Override
    public int rotateFamilyToken(String familyUuid, String oldHash, String newHash, long ttlMillis) {
        Long result = redisTemplate.execute(
            ROTATE_SCRIPT,
            List.of(familyKey(familyUuid)),
            oldHash, newHash, String.valueOf(ttlMillis)
        );
        int code = (result != null) ? result.intValue() : -1;

        if (code == -3) {
            log.warn("RefreshToken 재사용 감지 (Redis): familyUuid={}", familyUuid);
            jpaStore.revokeFamilyState(familyUuid, RevokeReason.REUSE_DETECTED);
        }
        return code;
    }

    @Override
    public void revokeFamilyState(String familyUuid, RevokeReason reason) {
        redisTemplate.opsForHash().put(familyKey(familyUuid), FIELD_REVOKED, REVOKED_TRUE);
        jpaStore.revokeFamilyState(familyUuid, reason);
        log.debug("Family 폐기: familyUuid={}, reason={}", familyUuid, reason);
    }

    @Override
    public int revokeAllFamilyStates(Long accountId, RevokeReason reason) {
        List<String> activeUuids = jpaStore.findActiveUuids(accountId);
        activeUuids.forEach(uuid ->
            redisTemplate.opsForHash().put(familyKey(uuid), FIELD_REVOKED, REVOKED_TRUE)
        );
        return jpaStore.revokeAllFamilyStates(accountId, reason);
    }

    private String familyKey(String familyUuid) {
        return KEY_PREFIX + familyUuid;
    }
}
