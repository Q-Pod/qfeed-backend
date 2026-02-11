package com.ktb.auth.service;

import com.ktb.auth.dto.jwt.RefreshTokenInfo;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenStore {

    /**
     * Refresh Token 저장
     *
     * @param familyId  Token Family ID
     * @param tokenHash 토큰 해시값
     * @param expiresAt 만료 시각
     */
    void save(Long familyId, String tokenHash, LocalDateTime expiresAt);

    /**
     * 토큰 해시로 Refresh Token 조회
     *
     * @param tokenHash 토큰 해시값
     * @return RefreshTokenInfo (Optional)
     */
    Optional<RefreshTokenInfo> findByTokenHash(String tokenHash);
}
