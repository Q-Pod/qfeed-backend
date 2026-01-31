package com.ktb.auth.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ktb.auth.dto.jwt.TokenClaims;
import com.ktb.auth.exception.token.InvalidAccessTokenException;
import com.ktb.auth.security.exception.AuthFailureException;
import com.ktb.auth.service.TokenService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationService 단위 테스트")
class JwtAuthenticationServiceTest {

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private JwtAuthenticationService authenticationService;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String EXPIRED_TOKEN = "expired.jwt.token";
    private static final Long USER_ID = 1L;
    private static final List<String> ROLES = List.of("ROLE_USER");
    private static final String USER_NICKNAME = "사용자";

    @Test
    @DisplayName("유효한 JWT로 인증 시 TokenClaims 반환")
    void authenticate_WithValidToken_ShouldReturnTokenClaims() {
        // given
        TokenClaims claims = new TokenClaims(USER_ID, USER_NICKNAME, ROLES);
        when(tokenService.validateAccessToken(VALID_TOKEN)).thenReturn(claims);

        // when
        Optional<TokenClaims> result = authenticationService.authenticate(VALID_TOKEN);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(USER_ID);
        assertThat(result.get().roles()).isEqualTo(ROLES);

        verify(tokenService).validateAccessToken(VALID_TOKEN);
    }

    @Test
    @DisplayName("만료된 JWT로 인증 시 AuthFailureException 발생")
    void authenticate_WithExpiredToken_ShouldThrowAuthFailureException() {
        // given
        when(tokenService.validateAccessToken(EXPIRED_TOKEN))
                .thenThrow(new InvalidAccessTokenException("토큰 만료"));

        // when & then
        assertThatThrownBy(() -> authenticationService.authenticate(EXPIRED_TOKEN))
                .isInstanceOf(AuthFailureException.class);

        verify(tokenService).validateAccessToken(EXPIRED_TOKEN);
    }

    @Test
    @DisplayName("변조된 JWT로 인증 시 AuthFailureException 발생")
    void authenticate_WithTamperedToken_ShouldThrowAuthFailureException() {
        // given
        String tamperedToken = "tampered.jwt.token";
        when(tokenService.validateAccessToken(tamperedToken))
                .thenThrow(new InvalidAccessTokenException("잘못된 서명"));

        // when & then
        assertThatThrownBy(() -> authenticationService.authenticate(tamperedToken))
                .isInstanceOf(AuthFailureException.class);
    }
}
