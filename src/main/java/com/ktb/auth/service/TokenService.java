package com.ktb.auth.service;

import com.ktb.auth.dto.jwt.RefreshTokenClaims;
import com.ktb.auth.dto.jwt.RefreshTokenEntity;
import com.ktb.auth.dto.jwt.TokenClaims;

import java.util.List;

public interface TokenService {

    /**
     * Access Token 발급
     */
    String issueAccessToken(Long accountId, List<String> roles, String nickname);

    /**
     * Access Token 검증
     */
    TokenClaims validateAccessToken(String accessToken);

    /**
     * Refresh Token 검증
     */
    RefreshTokenClaims validateRefreshToken(String refreshToken);

    /**
     * Refresh Token Hash로 DB 조회
     */
    RefreshTokenEntity findByTokenHash(String tokenHash);
}

