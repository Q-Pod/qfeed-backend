package com.ktb.auth.service;

import com.ktb.auth.domain.RefreshToken;
import com.ktb.auth.domain.TokenFamily;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.exception.token.InvalidAccessTokenException;
import com.ktb.auth.exception.token.InvalidRefreshTokenException;
import com.ktb.auth.jwt.JwtProperties;
import com.ktb.auth.jwt.JwtProvider;
import com.ktb.auth.repository.RefreshTokenRepository;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.auth.service.impl.TokenServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private JwtProperties jwtProperties;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

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
        UserAccount mockAccount = mock(UserAccount.class);
        when(mockAccount.getNickname()).thenReturn(USER_NICKNAME);
        when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(mockAccount));
        when(jwtProvider.createAccessToken(USER_ID, ROLES, USER_NICKNAME)).thenReturn(VALID_ACCESS_TOKEN);

        // when
        String accessToken = tokenService.issueAccessToken(USER_ID, ROLES);

        // then
        assertThat(accessToken).isEqualTo(VALID_ACCESS_TOKEN);
        verify(userAccountRepository).findById(USER_ID);
        verify(jwtProvider).createAccessToken(USER_ID, ROLES, USER_NICKNAME);
    }

    @Test
    @DisplayName("유효한 Access Token 검증이 성공해야 한다")
    void validateAccessToken_WithValidToken_ShouldSucceed() {
        // given
        Claims mockClaims = mock(Claims.class);
        when(mockClaims.get("userId", Long.class)).thenReturn(USER_ID);
        when(mockClaims.get("roles", List.class)).thenReturn(ROLES);
        when(jwtProvider.validateAccessToken(VALID_ACCESS_TOKEN)).thenReturn(mockClaims);

        // when
        TokenService.TokenClaims claims = tokenService.validateAccessToken(VALID_ACCESS_TOKEN);

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
        when(mockClaims.get("familyUuid", String.class)).thenReturn(FAMILY_UUID);
        when(jwtProvider.validateRefreshToken(VALID_REFRESH_TOKEN)).thenReturn(mockClaims);

        // when
        TokenService.RefreshTokenClaims claims = tokenService.validateRefreshToken(VALID_REFRESH_TOKEN);

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
    @DisplayName("Token Hash로 Refresh Token 조회가 성공해야 한다")
    void findByTokenHash_WithValidHash_ShouldSucceed() {
        // given
        TokenFamily mockFamily = mock(TokenFamily.class);
        when(mockFamily.getId()).thenReturn(1L);

        RefreshToken mockToken = mock(RefreshToken.class);
        when(mockToken.getId()).thenReturn(10L);
        when(mockToken.getFamily()).thenReturn(mockFamily);
        when(mockToken.getUsed()).thenReturn(false);
        when(mockToken.getExpiresAt()).thenReturn(LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByTokenHashWithFamily(TOKEN_HASH))
                .thenReturn(Optional.of(mockToken));

        // when
        TokenService.RefreshTokenEntity entity = tokenService.findByTokenHash(TOKEN_HASH);

        // then
        assertThat(entity.id()).isEqualTo(10L);
        assertThat(entity.familyId()).isEqualTo(1L);
        assertThat(entity.used()).isFalse();
        assertThat(entity.expiresAt()).isAfter(LocalDateTime.now());

        verify(refreshTokenRepository).findByTokenHashWithFamily(TOKEN_HASH);
    }

    @Test
    @DisplayName("존재하지 않는 Token Hash 조회 시 예외가 발생해야 한다")
    void findByTokenHash_WithNonExistentHash_ShouldThrowException() {
        // given
        when(refreshTokenRepository.findByTokenHashWithFamily(anyString()))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tokenService.findByTokenHash("nonexistent-hash"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository).findByTokenHashWithFamily("nonexistent-hash");
    }
}
