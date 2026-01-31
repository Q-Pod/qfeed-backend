package com.ktb.auth.service;

import com.ktb.auth.dto.AuthorizationUrlResult;
import com.ktb.auth.dto.OAuthExchangeCodeResult;
import com.ktb.auth.dto.OAuthLoginResult;
import com.ktb.auth.dto.jwt.TokenRefreshResult;

public interface OAuthApplicationService {

    /**
     * OAuth 인증 URL 생성
     */
    AuthorizationUrlResult getAuthorizationUrl(String provider);

    /**
     * OAuth 콜백 처리 (로그인/회원가입)
     */
    OAuthExchangeCodeResult handleCallback(String provider, String code, String state, String deviceInfo, String clientIp);

    /**
     * 교환 코드로 토큰 발급
     */
    OAuthLoginResult exchange(String exchangeCode);

    /**
     * 토큰 갱신 (RTR)
     */
    TokenRefreshResult refreshTokens(String refreshToken);

    /**
     * 로그아웃 (단일 기기)
     */
    void logout(Long accountId, String refreshToken);

    /**
     * 전체 기기 로그아웃
     */
    int logoutAll(Long accountId);
}
