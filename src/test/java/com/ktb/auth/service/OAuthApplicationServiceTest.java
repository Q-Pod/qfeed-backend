package com.ktb.auth.service;

import com.ktb.auth.client.KakaoOAuth2Client;
import com.ktb.auth.config.KakaoOAuthProviderProperties;
import com.ktb.auth.config.KakaoOAuthRegistrationProperties;
import com.ktb.auth.domain.OAuthProvider;
import com.ktb.auth.dto.jwt.RefreshTokenClaims;
import com.ktb.auth.dto.jwt.RefreshTokenEntity;
import com.ktb.auth.exception.oauth.OAuthProviderException;
import com.ktb.auth.domain.RefreshToken;
import com.ktb.auth.domain.RevokeReason;
import com.ktb.auth.domain.TokenFamily;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.dto.AuthorizationUrlResult;
import com.ktb.auth.dto.KakaoAccount;
import com.ktb.auth.dto.KakaoProfile;
import com.ktb.auth.dto.OAuthExchangeCodeResult;
import com.ktb.auth.dto.OAuthExchangePayload;
import com.ktb.auth.dto.response.KakaoUserInfoResponse;
import com.ktb.auth.dto.OAuthLoginResult;
import com.ktb.auth.dto.jwt.TokenRefreshResult;
import com.ktb.auth.exception.family.FamilyRevokedException;
import com.ktb.auth.exception.oauth.InvalidExchangeCodeException;
import com.ktb.auth.exception.oauth.InvalidStateException;
import com.ktb.auth.exception.token.TokenReuseDetectedException;
import com.ktb.auth.jwt.JwtProvider;
import com.ktb.auth.repository.RefreshTokenRepository;
import com.ktb.auth.repository.TokenFamilyRepository;
import com.ktb.auth.service.impl.OAuthApplicationServiceImpl;
import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.ktb.common.domain.ErrorCode.FAMILY_REVOKED;
import static com.ktb.common.domain.ErrorCode.TOKEN_REUSE_DETECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthApplicationService 단위 테스트 (Edge Cases 포함)")
class OAuthApplicationServiceTest {

    @Mock
    private KakaoOAuth2Client kakaoOAuth2Client;

    @Mock
    private OAuthDomainService oauthDomainService;

    @Mock
    private TokenService tokenService;

    @Mock
    private RTRService rtrService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenFamilyRepository tokenFamilyRepository;

    @Mock
    private KakaoOAuthProviderProperties kakaoProviderProperties;

    @Mock
    private KakaoOAuthRegistrationProperties kakaoRegistrationProperties;

    @InjectMocks
    private OAuthApplicationServiceImpl oauthApplicationService;

    private static final String KAKAO_PROVIDER = "kakao";
    private static final String INVALID_PROVIDER = "invalid";
    private static final String STATE = "state-123";
    private static final String CODE = "auth-code-123";
    private static final String DEVICE_INFO = "Chrome on MacOS";
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String KAKAO_ACCESS_TOKEN = "kakao.access.token";
    private static final String ACCESS_TOKEN = "access.token.jwt";
    private static final String REFRESH_TOKEN = "refresh.token.jwt";
    private static final String TOKEN_HASH = "token-hash-sha256";
    private static final String FAMILY_UUID = "family-uuid-123";
    private static final Long USER_ID = 1L;
    private static final String USER_NICKNAME = "신규유저";
    private static final Long FAMILY_ID = 10L;
    private static final String EXCHANGE_CODE = "exchange-code-123";

    @Test
    @DisplayName("Kakao Authorization URL 생성이 성공해야 한다")
    void getAuthorizationUrl_WithKakao_ShouldSucceed() {
        // given
        when(kakaoProviderProperties.getAuthorizationUri()).thenReturn("https://kauth.kakao.com/oauth/authorize");
        when(kakaoRegistrationProperties.getClientId()).thenReturn("test-client-id");
        when(kakaoRegistrationProperties.getRedirectUri()).thenReturn("http://localhost:8080/login/oauth2/code/kakao");
        when(kakaoRegistrationProperties.getScope()).thenReturn(List.of("profile_nickname", "profile_image", "account_email"));
        when(oauthDomainService.generateAndStoreState(KAKAO_PROVIDER)).thenReturn(STATE);

        // when
        AuthorizationUrlResult result = oauthApplicationService.getAuthorizationUrl(KAKAO_PROVIDER);

        // then
        assertThat(result.redirectUrl()).contains("https://kauth.kakao.com/oauth/authorize");
        assertThat(result.redirectUrl()).contains("client_id=test-client-id");
        assertThat(result.redirectUrl()).contains("state=" + STATE);
        verify(oauthDomainService).generateAndStoreState(KAKAO_PROVIDER);
    }

    @Test
    @DisplayName("지원하지 않는 Provider 요청 시 예외가 발생해야 한다")
    void getAuthorizationUrl_WithUnsupportedProvider_ShouldThrowException() {
        // when & then
        assertThatThrownBy(() -> oauthApplicationService.getAuthorizationUrl(INVALID_PROVIDER))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("지원하지 않는 OAuth 제공자입니다");

        verify(oauthDomainService, never()).generateAndStoreState(any());
    }

    @Test
    @DisplayName("OAuth 콜백 처리 - 신규 사용자 교환 코드 발급 성공")
    void handleCallback_WithNewUser_ShouldSucceed() {
        // given
        KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse(12345L, new KakaoAccount("newuser@example.com", new KakaoProfile("신규유저", null)));
        UserAccount newUser = mock(UserAccount.class);
        when(newUser.getId()).thenReturn(USER_ID);
        when(newUser.getNickname()).thenReturn("신규유저");
        when(newUser.getLastLoginAt()).thenReturn(null); // 신규 사용자

        doNothing().when(oauthDomainService).validateAndConsumeState(STATE);
        when(kakaoOAuth2Client.getAccessToken(CODE)).thenReturn(KAKAO_ACCESS_TOKEN);
        when(kakaoOAuth2Client.getUserInfo(KAKAO_ACCESS_TOKEN)).thenReturn(userInfo);
        when(oauthDomainService.findOrCreateAccount(OAuthProvider.KAKAO, "12345", userInfo)).thenReturn(newUser);
        when(oauthDomainService.generateAndStoreExchangeCode(any(OAuthExchangePayload.class))).thenReturn(EXCHANGE_CODE);

        // when
        OAuthExchangeCodeResult result = oauthApplicationService.handleCallback(
                KAKAO_PROVIDER, CODE, STATE, DEVICE_INFO, CLIENT_IP
        );

        // then
        assertThat(result.exchangeCode()).isEqualTo(EXCHANGE_CODE);

        verify(oauthDomainService).validateAndConsumeState(STATE);
        verify(newUser).updateLastLogin();
        verify(oauthDomainService).generateAndStoreExchangeCode(
                argThat(payload ->
                        payload.accountId().equals(USER_ID)
                                && payload.nickname().equals("신규유저")
                                && payload.isNewUser()
                                && payload.deviceInfo().equals(DEVICE_INFO)
                                && payload.clientIp().equals(CLIENT_IP)
                )
        );
    }

    @Test
    @DisplayName("OAuth 콜백 처리 - 기존 사용자 교환 코드 발급 성공")
    void handleCallback_WithExistingUser_ShouldSucceed() {
        // given
        KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse(12345L, new KakaoAccount("existing@example.com", new KakaoProfile("기존유저", null)));
        UserAccount existingUser = mock(UserAccount.class);
        when(existingUser.getId()).thenReturn(USER_ID);
        when(existingUser.getNickname()).thenReturn("기존유저");
        when(existingUser.getLastLoginAt()).thenReturn(LocalDateTime.now().minusDays(1)); // 기존 사용자

        doNothing().when(oauthDomainService).validateAndConsumeState(STATE);
        when(kakaoOAuth2Client.getAccessToken(CODE)).thenReturn(KAKAO_ACCESS_TOKEN);
        when(kakaoOAuth2Client.getUserInfo(KAKAO_ACCESS_TOKEN)).thenReturn(userInfo);
        when(oauthDomainService.findOrCreateAccount(OAuthProvider.KAKAO, "12345", userInfo)).thenReturn(existingUser);
        when(oauthDomainService.generateAndStoreExchangeCode(any(OAuthExchangePayload.class))).thenReturn(EXCHANGE_CODE);

        // when
        OAuthExchangeCodeResult result = oauthApplicationService.handleCallback(
                KAKAO_PROVIDER, CODE, STATE, DEVICE_INFO, CLIENT_IP
        );

        // then
        assertThat(result.exchangeCode()).isEqualTo(EXCHANGE_CODE);
        verify(existingUser).updateLastLogin();
    }

    @Test
    @DisplayName("[Edge Case] 잘못된 state로 콜백 시 예외가 발생해야 한다")
    void handleCallback_WithInvalidState_ShouldThrowException() {
        // given
        doThrow(new InvalidStateException())
                .when(oauthDomainService).validateAndConsumeState("invalid-state");

        // when & then
        assertThatThrownBy(() -> oauthApplicationService.handleCallback(KAKAO_PROVIDER, CODE, "invalid-state", DEVICE_INFO, CLIENT_IP))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining(ErrorCode.INVALID_STATE.getMessage());

        verify(kakaoOAuth2Client, never()).getAccessToken(any());
    }

    @Test
    @DisplayName("[Edge Case] OAuth Provider 통신 실패 시 예외가 발생해야 한다")
    void handleCallback_WithProviderError_ShouldThrowException() {
        // given
        doNothing().when(oauthDomainService).validateAndConsumeState(STATE);
        when(kakaoOAuth2Client.getAccessToken(CODE))
                .thenThrow(new OAuthProviderException());

        // when & then
        assertThatThrownBy(() -> oauthApplicationService.handleCallback(KAKAO_PROVIDER, CODE, STATE, DEVICE_INFO, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OAuth 제공자 통신에 실패했습니다");
    }

    @Test
    @DisplayName("교환 코드로 토큰 발급 성공")
    void exchange_WithValidCode_ShouldSucceed() {
        // given
        OAuthExchangePayload payload = new OAuthExchangePayload(USER_ID, USER_NICKNAME, true, DEVICE_INFO, CLIENT_IP);
        TokenFamily mockFamily = mock(TokenFamily.class);
        when(mockFamily.getUuid()).thenReturn(FAMILY_UUID);

        when(oauthDomainService.consumeExchangeCode(EXCHANGE_CODE)).thenReturn(payload);
        when(rtrService.createFamily(USER_ID, DEVICE_INFO, CLIENT_IP)).thenReturn(mockFamily);
        when(tokenService.issueAccessToken(USER_ID, List.of("ROLE_USER"), USER_NICKNAME)).thenReturn(ACCESS_TOKEN);
        when(jwtProvider.createRefreshToken(USER_ID, FAMILY_UUID, USER_NICKNAME)).thenReturn(REFRESH_TOKEN);
        when(jwtProvider.generateTokenHash(REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
        when(jwtProvider.refreshTokenDuration()).thenReturn(Duration.ofDays(14));

        // when
        OAuthLoginResult result = oauthApplicationService.exchange(EXCHANGE_CODE);

        // then
        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(result.user().nickname()).isEqualTo("신규유저");
        assertThat(result.user().isNewUser()).isTrue();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("[Edge Case] 만료되거나 잘못된 교환 코드면 예외가 발생해야 한다")
    void exchange_WithInvalidCode_ShouldThrowException() {
        // given
        when(oauthDomainService.consumeExchangeCode("invalid-code")).thenThrow(new InvalidExchangeCodeException());

        // when & then
        assertThatThrownBy(() -> oauthApplicationService.exchange("invalid-code"))
                .isInstanceOf(InvalidExchangeCodeException.class)
                .hasMessageContaining(ErrorCode.INVALID_EXCHANGE_CODE.getMessage());

        verify(rtrService, never()).createFamily(any(), any(), any());
    }

    @Test
    @DisplayName("Token Refresh (RTR) 성공")
    void refreshTokens_ShouldSucceed() {
        // given
        RefreshTokenClaims claims = new RefreshTokenClaims(USER_ID, USER_NICKNAME, FAMILY_UUID);
        RefreshTokenEntity tokenEntity = new RefreshTokenEntity(100L, FAMILY_ID, false, LocalDateTime.now().plusDays(7));
        TokenFamily mockFamily = mock(TokenFamily.class);

        when(tokenService.validateRefreshToken(REFRESH_TOKEN)).thenReturn(claims);
        when(jwtProvider.generateTokenHash(REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
        when(tokenService.findByTokenHash(TOKEN_HASH)).thenReturn(tokenEntity);
        doNothing().when(rtrService).detectReuse(tokenEntity);
        doNothing().when(rtrService).validateFamilyActive(FAMILY_ID);
        doNothing().when(rtrService).markAsUsed(100L);
        when(tokenService.issueAccessToken(USER_ID, List.of("ROLE_USER"), USER_NICKNAME)).thenReturn("new.access.token");
        when(jwtProvider.createRefreshToken(USER_ID, FAMILY_UUID, USER_NICKNAME)).thenReturn("new.refresh.token");
        when(tokenFamilyRepository.findByUuid(FAMILY_UUID)).thenReturn(Optional.of(mockFamily));
        when(jwtProvider.generateTokenHash("new.refresh.token")).thenReturn("new-token-hash");
        when(jwtProvider.refreshTokenDuration()).thenReturn(Duration.ofDays(14));
        when(jwtProvider.accessTokenExpiresSeconds()).thenReturn(600);

        // when
        TokenRefreshResult result = oauthApplicationService.refreshTokens(REFRESH_TOKEN);

        // then
        assertThat(result.accessToken()).isEqualTo("new.access.token");
        assertThat(result.refreshToken()).isEqualTo("new.refresh.token");
        assertThat(result.expiresIn()).isEqualTo(600);

        verify(rtrService).markAsUsed(100L);
        verify(mockFamily).updateLastUsed();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("[Edge Case] 재사용된 Refresh Token으로 갱신 시 Family가 폐기되어야 한다")
    void refreshTokens_WithReusedToken_ShouldRevokeFamily() {
        // given
        RefreshTokenClaims claims = new RefreshTokenClaims(USER_ID, FAMILY_UUID, USER_NICKNAME);
        RefreshTokenEntity usedToken = new RefreshTokenEntity(100L, FAMILY_ID, true, LocalDateTime.now().plusDays(7));

        when(tokenService.validateRefreshToken(REFRESH_TOKEN)).thenReturn(claims);
        when(jwtProvider.generateTokenHash(REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
        when(tokenService.findByTokenHash(TOKEN_HASH)).thenReturn(usedToken);
        doThrow(new TokenReuseDetectedException(FAMILY_ID))
                .when(rtrService).detectReuse(usedToken);

        // when & then
        assertThatThrownBy(() -> oauthApplicationService.refreshTokens(REFRESH_TOKEN))
                .isInstanceOf(TokenReuseDetectedException.class)
                .hasMessageContaining(TOKEN_REUSE_DETECTED.getMessage());

        verify(rtrService).detectReuse(usedToken);
        verify(tokenService, never()).issueAccessToken(any(), any(), any());
    }

    @Test
    @DisplayName("로그아웃 (단일 기기) 성공")
    void logout_ShouldSucceed() {
        // given
        RefreshTokenClaims claims = new RefreshTokenClaims(USER_ID, USER_NICKNAME, FAMILY_UUID);
        TokenFamily mockFamily = mock(TokenFamily.class);

        when(tokenService.validateRefreshToken(REFRESH_TOKEN)).thenReturn(claims);
        when(tokenFamilyRepository.findByUuid(FAMILY_UUID)).thenReturn(Optional.of(mockFamily));

        // when
        oauthApplicationService.logout(USER_ID, REFRESH_TOKEN);

        // then
        verify(mockFamily).revoke(RevokeReason.USER_LOGOUT);
    }

    @Test
    @DisplayName("[Edge Case] 다른 사용자의 Refresh Token으로 로그아웃 시도 시 예외가 발생해야 한다")
    void logout_WithDifferentUser_ShouldThrowException() {
        // given
        Long otherUserId = 999L;
        RefreshTokenClaims claims = new RefreshTokenClaims(otherUserId, FAMILY_UUID, USER_NICKNAME);

        when(tokenService.validateRefreshToken(REFRESH_TOKEN)).thenReturn(claims);

        // when & then
        assertThatThrownBy(() -> oauthApplicationService.logout(USER_ID, REFRESH_TOKEN))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("세션 소유권이 일치하지 않습니다");

        verify(tokenFamilyRepository, never()).findByUuid(any());
    }

    @Test
    @DisplayName("전체 기기 로그아웃 성공")
    void logoutAll_ShouldSucceed() {
        // given
        when(tokenFamilyRepository.revokeAllByAccountId(USER_ID, RevokeReason.USER_LOGOUT)).thenReturn(3);

        // when
        int revokedCount = oauthApplicationService.logoutAll(USER_ID);

        // then
        assertThat(revokedCount).isEqualTo(3);
        verify(tokenFamilyRepository).revokeAllByAccountId(USER_ID, RevokeReason.USER_LOGOUT);
    }

    @Test
    @DisplayName("[Edge Case] 폐기된 Family의 Refresh Token 사용 시 예외가 발생해야 한다")
    void refreshTokens_WithRevokedFamily_ShouldThrowException() {
        // given
        RefreshTokenClaims claims = new RefreshTokenClaims(USER_ID, FAMILY_UUID, USER_NICKNAME);
        RefreshTokenEntity tokenEntity = new RefreshTokenEntity(100L, FAMILY_ID, false, LocalDateTime.now().plusDays(7));

        when(tokenService.validateRefreshToken(REFRESH_TOKEN)).thenReturn(claims);
        when(jwtProvider.generateTokenHash(REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
        when(tokenService.findByTokenHash(TOKEN_HASH)).thenReturn(tokenEntity);
        doNothing().when(rtrService).detectReuse(tokenEntity);
        doThrow(new FamilyRevokedException(FAMILY_ID))
                .when(rtrService).validateFamilyActive(FAMILY_ID);

        // when & then
        assertThatThrownBy(() -> oauthApplicationService.refreshTokens(REFRESH_TOKEN))
                .isInstanceOf(FamilyRevokedException.class)
                .hasMessageContaining(FAMILY_REVOKED.getMessage());

        verify(rtrService).validateFamilyActive(FAMILY_ID);
    }
}
