package com.ktb.auth.service;

import com.ktb.auth.dto.CookieSpec;
import com.ktb.auth.jwt.JwtProperties;
import com.ktb.auth.service.impl.CookieServiceImpl;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CookieService 단위 테스트")
class CookieServiceTest {

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private CookieServiceImpl cookieService;

    private static final String COOKIE_NAME = "refreshToken";
    private static final Long REFRESH_TOKEN_EXPIRATION = 1209600000L; // 14일 (밀리초)
    private static final String SAMPLE_REFRESH_TOKEN = "sample.refresh.token";

    @BeforeEach
    void setUp() {
        when(jwtProperties.getRefreshTokenCookieName()).thenReturn(COOKIE_NAME);
    }

    @Nested
    @DisplayName("프로덕션 환경 (cookie.secure=true)")
    class SecureEnvironment {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(cookieService, "cookieSecure", true);
        }

        @Test
        @DisplayName("Refresh Token 쿠키 스펙 생성 시 Secure=true, SameSite=Strict 설정")
        void createRefreshTokenCookie_ShouldSetSecureAndStrictSameSite() {
            // given
            when(jwtProperties.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);

            // when
            CookieSpec spec = cookieService.createRefreshTokenCookie(SAMPLE_REFRESH_TOKEN);

            // then
            assertThat(spec.name()).isEqualTo(COOKIE_NAME);
            assertThat(spec.value()).isEqualTo(SAMPLE_REFRESH_TOKEN);
            assertThat(spec.httpOnly()).isTrue();
            assertThat(spec.secure()).isTrue();
            assertThat(spec.path()).isEqualTo("/api/auth");
            assertThat(spec.maxAgeSeconds()).isEqualTo((int) (REFRESH_TOKEN_EXPIRATION / 1000));
            assertThat(spec.sameSite()).isEqualTo("Strict");
        }

        @Test
        @DisplayName("CookieSpec에서 Servlet Cookie로 변환이 성공해야 한다")
        void toServletCookie_ShouldConvertCorrectly() {
            // given
            when(jwtProperties.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);
            CookieSpec spec = cookieService.createRefreshTokenCookie(SAMPLE_REFRESH_TOKEN);

            // when
            Cookie cookie = spec.toServletCookie();

            // then
            assertThat(cookie.getName()).isEqualTo(COOKIE_NAME);
            assertThat(cookie.getValue()).isEqualTo(SAMPLE_REFRESH_TOKEN);
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.getSecure()).isTrue();
            assertThat(cookie.getPath()).isEqualTo("/api/auth");
            assertThat(cookie.getMaxAge()).isEqualTo((int) (REFRESH_TOKEN_EXPIRATION / 1000));
            assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
        }

        @Test
        @DisplayName("만료된 Refresh Token 쿠키 스펙 생성 시 Secure=true, SameSite=Strict 설정")
        void createExpiredRefreshTokenCookie_ShouldSetSecureAndStrictSameSite() {
            // when
            CookieSpec spec = cookieService.createExpiredRefreshTokenCookie();

            // then
            assertThat(spec.name()).isEqualTo(COOKIE_NAME);
            assertThat(spec.value()).isEmpty();
            assertThat(spec.httpOnly()).isTrue();
            assertThat(spec.secure()).isTrue();
            assertThat(spec.path()).isEqualTo("/api/auth");
            assertThat(spec.maxAgeSeconds()).isZero();
            assertThat(spec.sameSite()).isEqualTo("Strict");
        }
    }

    @Nested
    @Disabled
    @DisplayName("로컬 환경 (cookie.secure=false)")
    class LocalEnvironment {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(cookieService, "cookieSecure", false);
        }

        @Test
        @DisplayName("Refresh Token 쿠키 스펙 생성 시 Secure=false, SameSite=Lax 설정")
        void createRefreshTokenCookie_ShouldSetInsecureAndLaxSameSite() {
            // given
            when(jwtProperties.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);

            // when
            CookieSpec spec = cookieService.createRefreshTokenCookie(SAMPLE_REFRESH_TOKEN);

            // then
            assertThat(spec.name()).isEqualTo(COOKIE_NAME);
            assertThat(spec.value()).isEqualTo(SAMPLE_REFRESH_TOKEN);
            assertThat(spec.httpOnly()).isTrue();
            assertThat(spec.secure()).isFalse();
            assertThat(spec.path()).isEqualTo("/api/auth");
            assertThat(spec.maxAgeSeconds()).isEqualTo((int) (REFRESH_TOKEN_EXPIRATION / 1000));
            assertThat(spec.sameSite()).isEqualTo("Lax");
        }

        @Test
        @DisplayName("만료된 Refresh Token 쿠키 스펙 생성 시 Secure=false, SameSite=Lax 설정")
        void createExpiredRefreshTokenCookie_ShouldSetInsecureAndLaxSameSite() {
            // when
            CookieSpec spec = cookieService.createExpiredRefreshTokenCookie();

            // then
            assertThat(spec.name()).isEqualTo(COOKIE_NAME);
            assertThat(spec.value()).isEmpty();
            assertThat(spec.httpOnly()).isTrue();
            assertThat(spec.secure()).isFalse();
            assertThat(spec.path()).isEqualTo("/api/auth");
            assertThat(spec.maxAgeSeconds()).isZero();
            assertThat(spec.sameSite()).isEqualTo("Lax");
        }
    }

    @Test
    @DisplayName("Refresh Token 쿠키의 MaxAge는 초 단위로 변환되어야 한다")
    void createRefreshTokenCookie_ShouldConvertMaxAgeToSeconds() {
        // given
        ReflectionTestUtils.setField(cookieService, "cookieSecure", true);
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);

        // when
        CookieSpec spec = cookieService.createRefreshTokenCookie(SAMPLE_REFRESH_TOKEN);

        // then
        assertThat(spec.maxAgeSeconds()).isEqualTo(1209600);
    }

    @Test
    @DisplayName("만료된 쿠키와 일반 쿠키의 보안 속성이 동일해야 한다")
    void createExpiredRefreshTokenCookie_ShouldHaveSameSecurityAttributes() {
        // given
        ReflectionTestUtils.setField(cookieService, "cookieSecure", true);
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);

        // when
        CookieSpec expiredSpec = cookieService.createExpiredRefreshTokenCookie();
        CookieSpec normalSpec = cookieService.createRefreshTokenCookie(SAMPLE_REFRESH_TOKEN);

        // then
        assertThat(expiredSpec.httpOnly()).isEqualTo(normalSpec.httpOnly());
        assertThat(expiredSpec.secure()).isEqualTo(normalSpec.secure());
        assertThat(expiredSpec.path()).isEqualTo(normalSpec.path());
        assertThat(expiredSpec.sameSite()).isEqualTo(normalSpec.sameSite());
    }
}
