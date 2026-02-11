package com.ktb.auth.service;

import com.ktb.auth.dto.jwt.RefreshTokenClaims;
import com.ktb.auth.dto.jwt.RefreshTokenInfo;
import com.ktb.auth.dto.jwt.TokenClaims;

import java.time.Duration;
import java.util.List;

public interface TokenService {

    /**
     * Access Token 발급
     */
    String issueAccessToken(Long accountId, List<String> roles, String nickname);

    /**
     * Refresh Token 발급
     */
    String issueRefreshToken(Long accountId, String familyUuid, String nickname);

    /**
     * Access Token 검증
     */
    TokenClaims validateAccessToken(String accessToken);

    /**
     * Refresh Token 검증
     */
    RefreshTokenClaims validateRefreshToken(String refreshToken);

    /**
     * Refresh Token 저장
     *
     * @param familyId     Token Family ID
     * @param refreshToken Refresh Token 문자열
     */
    void storeRefreshToken(Long familyId, String refreshToken);

    /**
     * 저장된 Refresh Token 조회
     *
     * @param refreshToken Refresh Token 문자열
     * @return RefreshTokenInfo
     */
    RefreshTokenInfo getStoredRefreshToken(String refreshToken);

    /**
     * 토큰 해시 생성
     */
    String generateTokenHash(String token);

    /**
     * Refresh Token 유효 기간 조회
     */
    Duration getRefreshTokenDuration();

    /**
     * Access Token 만료 시간(초) 조회
     */
    long getAccessTokenExpiresSeconds();
}

