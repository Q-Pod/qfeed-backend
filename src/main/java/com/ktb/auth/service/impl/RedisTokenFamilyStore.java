package com.ktb.auth.service.impl;

import com.ktb.auth.dto.TokenFamilyInfo;
import com.ktb.auth.service.TokenFamilyStore;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Redis 기반 Token Family 저장소 구현체 (캐시 레이어)
 * Redis 프로파일 활성화 시 사용
 * <p>
 * TODO: Redis 도입 시 구현 완료 필요
 * - RedisTemplate 주입
 * - Cache-Aside 패턴 적용
 * - JpaTokenFamilyStore를 fallback으로 사용
 */
@Slf4j
@Service
@Profile("redis")
@RequiredArgsConstructor
public class RedisTokenFamilyStore implements TokenFamilyStore {

    private final JpaTokenFamilyStore jpaStore;

    @Override
    public Optional<TokenFamilyInfo> findByUuid(String uuid) {
        log.debug("Redis 캐시에서 Token Family 조회 (미구현, JPA fallback): uuid={}", uuid);
        return jpaStore.findByUuid(uuid);
    }

    @Override
    public void updateLastUsed(String uuid) {
        jpaStore.updateLastUsed(uuid);
        log.debug("Redis 캐시에 Token Family 마지막 사용 시각 업데이트 (미구현, JPA fallback): uuid={}", uuid);
    }
}
