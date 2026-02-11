package com.ktb.auth.service;

import com.ktb.auth.dto.jwt.RefreshTokenClaims;
import com.ktb.auth.dto.jwt.RefreshTokenInfo;
import com.ktb.auth.dto.jwt.TokenClaims;
import com.ktb.auth.exception.token.InvalidAccessTokenException;
import com.ktb.auth.exception.token.InvalidRefreshTokenException;
import com.ktb.auth.jwt.JwtProvider;
import com.ktb.auth.service.impl.TokenServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService 단위 테스트")
class TokenServiceTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private static final Long USER_ID = 1L;
    private static final String FAMILY_UUID = "family-uuid-123";
    private static final List<String> ROLES = List.of("ROLE_USER");
    private static final String VALID_ACCESS_TOKEN = "valid.access.token";
    private static final String VALID_REFRESH_TOKEN = "valid.refresh.token";
    private static final String EXPIRED_TOKEN = "expired.token";
    private static final String TAMPERED_TOKEN = "tampered.token";
    private static final String TOKEN_HASH = "token-hash-sha256";
    private static final String USER_NICKNAME = "테스트유저";

    @Test
    @DisplayName("Access Token 발급이 성공해야 한다")
    void issueAccessToken_ShouldSucceed() {
        // given
        when(jwtProvider.createAccessToken(USER_ID, ROLES, USER_NICKNAME)).thenReturn(VALID_ACCESS_TOKEN);

        // when
        String accessToken = tokenService.issueAccessToken(USER_ID, ROLES, USER_NICKNAME);

        // then
        assertThat(accessToken).isEqualTo(VALID_ACCESS_TOKEN);
        verify(jwtProvider).createAccessToken(USER_ID, ROLES, USER_NICKNAME);
    }

    @Test
    @DisplayName("유효한 Access Token 검증이 성공해야 한다")
    void validateAccessToken_WithValidToken_ShouldSucceed() {
        // given
        Claims mockClaims = mock(Claims.class);
        when(mockClaims.get("userId", Long.class)).thenReturn(USER_ID);
        when(mockClaims.get("nickname", String.class)).thenReturn(USER_NICKNAME);
        when(mockClaims.get("roles", List.class)).thenReturn(ROLES);
        when(jwtProvider.validateAccessToken(VALID_ACCESS_TOKEN)).thenReturn(mockClaims);

        // when
        TokenClaims claims = tokenService.validateAccessToken(VALID_ACCESS_TOKEN);

        // then
        assertThat(claims.userId()).isEqualTo(USER_ID);
        assertThat(claims.roles()).isEqualTo(ROLES);
        verify(jwtProvider).validateAccessToken(VALID_ACCESS_TOKEN);
    }

    @Test
    @DisplayName("만료된 Access Token 검증 시 예외가 발생해야 한다")
    void validateAccessToken_WithExpiredToken_ShouldThrowException() {
        // given
        when(jwtProvider.validateAccessToken(EXPIRED_TOKEN))
                .thenThrow(new ExpiredJwtException(null, null, "Access Token이 만료되었습니다."));

        // when & then
        assertThatThrownBy(() -> tokenService.validateAccessToken(EXPIRED_TOKEN))
                .isInstanceOf(InvalidAccessTokenException.class);

        verify(jwtProvider).validateAccessToken(EXPIRED_TOKEN);
    }

    @Test
    @DisplayName("변조된 Access Token 검증 시 예외가 발생해야 한다")
    void validateAccessToken_WithTamperedToken_ShouldThrowException() {
        // given
        when(jwtProvider.validateAccessToken(TAMPERED_TOKEN))
                .thenThrow(new SecurityException("잘못된 Access Token 서명입니다."));

        // when & then
        assertThatThrownBy(() -> tokenService.validateAccessToken(TAMPERED_TOKEN))
                .isInstanceOf(InvalidAccessTokenException.class);

        verify(jwtProvider).validateAccessToken(TAMPERED_TOKEN);
    }

    @Test
    @DisplayName("유효한 Refresh Token 검증이 성공해야 한다")
    void validateRefreshToken_WithValidToken_ShouldSucceed() {
        // given
        Claims mockClaims = mock(Claims.class);
        when(mockClaims.get("userId", Long.class)).thenReturn(USER_ID);
        when(mockClaims.get("nickname", String.class)).thenReturn(USER_NICKNAME);
        when(mockClaims.get("familyUuid", String.class)).thenReturn(FAMILY_UUID);
        when(jwtProvider.validateRefreshToken(VALID_REFRESH_TOKEN)).thenReturn(mockClaims);

        // when
        RefreshTokenClaims claims = tokenService.validateRefreshToken(VALID_REFRESH_TOKEN);

        // then
        assertThat(claims.userId()).isEqualTo(USER_ID);
        assertThat(claims.familyUuid()).isEqualTo(FAMILY_UUID);
        verify(jwtProvider).validateRefreshToken(VALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("만료된 Refresh Token 검증 시 예외가 발생해야 한다")
    void validateRefreshToken_WithExpiredToken_ShouldThrowException() {
        // given
        when(jwtProvider.validateRefreshToken(EXPIRED_TOKEN))
                .thenThrow(new ExpiredJwtException(null, null, "Refresh Token이 만료되었습니다."));

        // when & then
        assertThatThrownBy(() -> tokenService.validateRefreshToken(EXPIRED_TOKEN))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(jwtProvider).validateRefreshToken(EXPIRED_TOKEN);
    }

    @Test
    @DisplayName("Refresh Token 발급이 성공해야 한다")
    void issueRefreshToken_ShouldSucceed() {
        // given
        when(jwtProvider.createRefreshToken(USER_ID, FAMILY_UUID, USER_NICKNAME)).thenReturn(VALID_REFRESH_TOKEN);

        // when
        String refreshToken = tokenService.issueRefreshToken(USER_ID, FAMILY_UUID, USER_NICKNAME);

        // then
        assertThat(refreshToken).isEqualTo(VALID_REFRESH_TOKEN);
        verify(jwtProvider).createRefreshToken(USER_ID, FAMILY_UUID, USER_NICKNAME);
    }

    @Test
    @DisplayName("저장된 Refresh Token 조회가 성공해야 한다")
    void getStoredRefreshToken_WithValidToken_ShouldSucceed() {
        // given
        RefreshTokenInfo expectedInfo = new RefreshTokenInfo(10L, 1L, false, LocalDateTime.now().plusDays(7));

        when(jwtProvider.generateTokenHash(VALID_REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
        when(refreshTokenStore.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(expectedInfo));

        // when
        RefreshTokenInfo info = tokenService.getStoredRefreshToken(VALID_REFRESH_TOKEN);

        // then
        assertThat(info.id()).isEqualTo(10L);
        assertThat(info.familyId()).isEqualTo(1L);
        assertThat(info.used()).isFalse();
        assertThat(info.expiresAt()).isAfter(LocalDateTime.now());

        verify(jwtProvider).generateTokenHash(VALID_REFRESH_TOKEN);
        verify(refreshTokenStore).findByTokenHash(TOKEN_HASH);
    }

    @Test
    @DisplayName("존재하지 않는 Refresh Token 조회 시 예외가 발생해야 한다")
    void getStoredRefreshToken_WithNonExistentToken_ShouldThrowException() {
        // given
        when(jwtProvider.generateTokenHash(anyString())).thenReturn("nonexistent-hash");
        when(refreshTokenStore.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tokenService.getStoredRefreshToken("nonexistent-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenStore).findByTokenHash("nonexistent-hash");
    }

    @Test
    @DisplayName("Refresh Token 저장이 성공해야 한다")
    void storeRefreshToken_ShouldSucceed() {
        // given
        Long familyId = 1L;
        when(jwtProvider.generateTokenHash(VALID_REFRESH_TOKEN)).thenReturn(TOKEN_HASH);
        when(jwtProvider.refreshTokenDuration()).thenReturn(Duration.ofDays(14));

        // when
        tokenService.storeRefreshToken(familyId, VALID_REFRESH_TOKEN);

        // then
        verify(jwtProvider).generateTokenHash(VALID_REFRESH_TOKEN);
        verify(refreshTokenStore).save(eq(familyId), eq(TOKEN_HASH), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("토큰 해시 생성이 성공해야 한다")
    void generateTokenHash_ShouldSucceed() {
        // given
        when(jwtProvider.generateTokenHash(VALID_ACCESS_TOKEN)).thenReturn(TOKEN_HASH);

        // when
        String hash = tokenService.generateTokenHash(VALID_ACCESS_TOKEN);

        // then
        assertThat(hash).isEqualTo(TOKEN_HASH);
        verify(jwtProvider).generateTokenHash(VALID_ACCESS_TOKEN);
    }

    @Test
    @DisplayName("Refresh Token 유효 기간 조회가 성공해야 한다")
    void getRefreshTokenDuration_ShouldSucceed() {
        // given
        Duration expectedDuration = Duration.ofDays(14);
        when(jwtProvider.refreshTokenDuration()).thenReturn(expectedDuration);

        // when
        Duration duration = tokenService.getRefreshTokenDuration();

        // then
        assertThat(duration).isEqualTo(expectedDuration);
        verify(jwtProvider).refreshTokenDuration();
    }

    @Test
    @DisplayName("Access Token 만료 시간 조회가 성공해야 한다")
    void getAccessTokenExpiresSeconds_ShouldSucceed() {
        // given
        when(jwtProvider.accessTokenExpiresSeconds()).thenReturn(600);

        // when
        long expiresSeconds = tokenService.getAccessTokenExpiresSeconds();

        // then
        assertThat(expiresSeconds).isEqualTo(600);
        verify(jwtProvider).accessTokenExpiresSeconds();
    }
}
