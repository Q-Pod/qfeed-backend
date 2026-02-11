package com.ktb.auth.service.impl;

import com.ktb.auth.dto.jwt.RefreshTokenInfo;
import com.ktb.auth.service.RefreshTokenStore;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Redis 기반 Refresh Token 저장소 구현체 (캐시 레이어)
 * Redis 프로파일 활성화 시 사용
 * <p>
 * TODO: Redis 도입 시 구현 완료 필요
 * - RedisTemplate 주입
 * - Cache-Aside 패턴 적용
 * - JpaRefreshTokenStore를 fallback으로 사용
 */
@Slf4j
@Service
@Profile("redis")
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private final JpaRefreshTokenStore jpaStore;

    @Override
    public void save(Long familyId, String tokenHash, LocalDateTime expiresAt) {
        jpaStore.save(familyId, tokenHash, expiresAt);
        log.debug("Redis 캐시에 Refresh Token 저장 (미구현, JPA fallback): familyId={}", familyId);
    }

    @Override
    public Optional<RefreshTokenInfo> findByTokenHash(String tokenHash) {
        log.debug("Redis 캐시에서 Refresh Token 조회 (미구현, JPA fallback): tokenHash={}", tokenHash);
        return jpaStore.findByTokenHash(tokenHash);
    }
}
