package com.ktb.auth.service.impl;

import com.ktb.auth.client.KakaoOAuth2Client;
import com.ktb.auth.config.KakaoOAuthProviderProperties;
import com.ktb.auth.config.KakaoOAuthRegistrationProperties;
import com.ktb.auth.domain.OAuthProvider;
import com.ktb.auth.domain.RevokeReason;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.dto.AuthorizationUrlResult;
import com.ktb.auth.dto.OAuthExchangeCodeResult;
import com.ktb.auth.dto.OAuthExchangePayload;
import com.ktb.auth.dto.OAuthLoginResult;
import com.ktb.auth.dto.TokenFamilyInfo;
import com.ktb.auth.dto.UserInfo;
import com.ktb.auth.dto.jwt.RefreshTokenClaims;
import com.ktb.auth.dto.jwt.TokenRefreshResult;
import com.ktb.auth.dto.response.KakaoUserInfoResponse;
import com.ktb.auth.exception.family.FamilyOwnershipException;
import com.ktb.auth.exception.oauth.OAuthProviderException;
import com.ktb.auth.exception.oauth.UnsupportedProviderException;
import com.ktb.auth.exception.family.FamilyRevokedException;
import com.ktb.auth.exception.token.InvalidRefreshTokenException;
import com.ktb.auth.exception.token.TokenReuseDetectedException;
import com.ktb.auth.service.OAuthApplicationService;
import com.ktb.auth.service.OAuthDomainService;
import com.ktb.auth.service.RTRService;
import com.ktb.auth.service.TokenFamilyStore;
import com.ktb.auth.service.TokenService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthApplicationServiceImpl implements OAuthApplicationService {

    private static final String SUPPORTED_PROVIDER_KAKAO = "kakao";
    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final List<String> DEFAULT_ROLES = List.of(DEFAULT_ROLE);

    private final KakaoOAuth2Client kakaoOAuth2Client;
    private final OAuthDomainService oauthDomainService;
    private final TokenService tokenService;
    private final RTRService rtrService;
    private final TokenFamilyStore tokenFamilyStore;
    private final KakaoOAuthProviderProperties kakaoProviderProperties;
    private final KakaoOAuthRegistrationProperties kakaoRegistrationProperties;

    @Override
    public AuthorizationUrlResult getAuthorizationUrl(String provider) {
        if (!SUPPORTED_PROVIDER_KAKAO.equalsIgnoreCase(provider)) {
            throw new UnsupportedProviderException(provider);
        }

        String state = oauthDomainService.generateAndStoreState(provider);

        List<String> scopes = kakaoRegistrationProperties.getScope();
        String scopeParam = String.join(",", scopes == null ? List.of() : scopes);

        String redirectUrl = String.format(
                "%s?client_id=%s&redirect_uri=%s&response_type=code&state=%s&scope=%s",
                kakaoProviderProperties.getAuthorizationUri(),
                kakaoRegistrationProperties.getClientId(),
                kakaoRegistrationProperties.getRedirectUri(),
                state,
                scopeParam
        );

        return new AuthorizationUrlResult(redirectUrl);
    }

    @Override
    @Transactional
    public OAuthExchangeCodeResult handleCallback(String provider, String code, String state, String deviceInfo, String clientIp) {
        if (!SUPPORTED_PROVIDER_KAKAO.equalsIgnoreCase(provider)) {
            throw new UnsupportedProviderException(provider);
        }

        oauthDomainService.validateAndConsumeState(state);

        try {
            String kakaoAccessToken = kakaoOAuth2Client.getAccessToken(code);

            KakaoUserInfoResponse userInfo = kakaoOAuth2Client.getUserInfo(kakaoAccessToken);

            UserAccount account = oauthDomainService.findOrCreateAccount(
                    OAuthProvider.KAKAO,
                    userInfo.getProviderId(),
                    userInfo
            );

            boolean isNewUser = account.getLastLoginAt() == null;

            account.updateLastLogin();

            OAuthExchangePayload payload = new OAuthExchangePayload(
                    account.getId(),
                    account.getNickname(),
                    isNewUser,
                    deviceInfo,
                    clientIp
            );
            String exchangeCode = oauthDomainService.generateAndStoreExchangeCode(payload);

            log.info("OAuth 로그인 성공: accountId={}, isNewUser={}", account.getId(), isNewUser);

            return new OAuthExchangeCodeResult(exchangeCode);

        } catch (Exception e) {
            log.error("OAuth 로그인 실패: provider={}, error={}", provider, e.getMessage(), e);
            throw new OAuthProviderException();
        }
    }

    @Override
    @Transactional
    public OAuthLoginResult exchange(String exchangeCode) {
        OAuthExchangePayload payload = oauthDomainService.consumeExchangeCode(exchangeCode);

        TokenFamilyInfo family = rtrService.createFamily(
                payload.accountId(),
                payload.deviceInfo(),
                payload.clientIp()
        );

        String accessToken = tokenService.issueAccessToken(
                payload.accountId(),
                DEFAULT_ROLES,
                payload.nickname()
        );
        String refreshToken = tokenService.issueRefreshToken(
                payload.accountId(),
                family.uuid(),
                payload.nickname()
        );

        long ttlMillis = tokenService.getRefreshTokenDuration().toMillis();
        tokenFamilyStore.initFamilyState(family.uuid(), tokenService.generateTokenHash(refreshToken), ttlMillis);

        return new OAuthLoginResult(
                accessToken,
                refreshToken,
                new UserInfo(payload.nickname(), payload.isNewUser())
        );
    }

    @Override
    public TokenRefreshResult refreshTokens(String refreshToken) {
        RefreshTokenClaims claims = tokenService.validateRefreshToken(refreshToken);

        String oldHash = tokenService.generateTokenHash(refreshToken);
        String newRefreshToken = tokenService.issueRefreshToken(
                claims.userId(),
                claims.familyUuid(),
                claims.userNickname()
        );
        String newHash = tokenService.generateTokenHash(newRefreshToken);
        long ttlMillis = tokenService.getRefreshTokenDuration().toMillis();

        int result = tokenFamilyStore.rotateFamilyToken(
                claims.familyUuid(), oldHash, newHash, ttlMillis
        );

        switch (result) {
            case 1 -> { }
            case -1 -> throw new InvalidRefreshTokenException();
            case -2 -> throw new FamilyRevokedException();
            case -3 -> throw new TokenReuseDetectedException(claims.familyUuid());
        }

        String newAccessToken = tokenService.issueAccessToken(
                claims.userId(),
                DEFAULT_ROLES,
                claims.userNickname()
        );

        log.info("Token Refresh 성공: userId={}, familyUuid={}", claims.userId(), claims.familyUuid());

        return new TokenRefreshResult(
                newAccessToken,
                newRefreshToken,
                (int) tokenService.getAccessTokenExpiresSeconds()
        );
    }

    @Override
    @Transactional
    public void logout(Long accountId, String refreshToken) {
        RefreshTokenClaims claims = tokenService.validateRefreshToken(refreshToken);

        if (!claims.userId().equals(accountId)) {
            throw new FamilyOwnershipException();
        }

        tokenFamilyStore.revokeFamilyState(claims.familyUuid(), RevokeReason.USER_LOGOUT);

        log.info("로그아웃 성공: accountId={}, familyUuid={}", accountId, claims.familyUuid());
    }

    @Override
    @Transactional
    public int logoutAll(Long accountId) {
        int count = tokenFamilyStore.revokeAllFamilyStates(accountId, RevokeReason.USER_LOGOUT);

        log.info("전체 로그아웃 성공: accountId={}, revokedCount={}", accountId, count);
        return count;
    }
}
