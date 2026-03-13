package com.ktb.auth.service;

import com.ktb.auth.domain.RevokeReason;
import com.ktb.auth.dto.TokenFamilyInfo;
import java.util.Optional;

public interface TokenFamilyStore {

    /**
     * UUID로 Token Family 조회
     */
    Optional<TokenFamilyInfo> findByUuid(String uuid);

    /**
     * 로그인 시 Family 초기 상태 등록 (currentHash, revoked=0)
     *
     * @param familyUuid Family UUID
     * @param tokenHash  최초 refresh token 해시
     * @param ttlMillis  TTL (밀리초)
     */
    void initFamilyState(String familyUuid, String tokenHash, long ttlMillis);

    /**
     * 토큰 교체 (RTR Rotate) — 원자적 compare-and-swap
     *
     * @return 1=성공, -1=key 없음(만료), -2=revoked, -3=재사용 감지(revoke 처리됨)
     */
    int rotateFamilyToken(String familyUuid, String oldHash, String newHash, long ttlMillis);

    /**
     * Family 폐기 (로그아웃, 재사용 감지 시)
     */
    void revokeFamilyState(String familyUuid, RevokeReason reason);

    /**
     * 계정의 모든 Family 폐기 (전체 로그아웃)
     *
     * @return 폐기된 Family 수
     */
    int revokeAllFamilyStates(Long accountId, RevokeReason reason);
}
