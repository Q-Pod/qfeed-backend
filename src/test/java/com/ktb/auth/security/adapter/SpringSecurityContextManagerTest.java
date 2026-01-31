package com.ktb.auth.security.adapter;

import com.ktb.auth.dto.jwt.TokenClaims;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("SpringSecurityContextManager 단위 테스트")
class SpringSecurityContextManagerTest {

    private SpringSecurityContextManager contextManager;

    private static final String USER_NICKNAME = "사용자";

    @BeforeEach
    void setUp() {
        contextManager = new SpringSecurityContextManager();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 정보 설정 성공")
    void setAuthentication_ShouldSetSecurityContext() {
        // given
        TokenClaims claims = new TokenClaims(1L, USER_NICKNAME, List.of("ROLE_USER"));
        HttpServletRequest request = mock(HttpServletRequest.class);

        // when
        contextManager.setAuthentication(claims, request);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isInstanceOf(SecurityUserAccount.class);
    }

    @Test
    @DisplayName("인증 컨텍스트 초기화 성공")
    void clearAuthentication_ShouldClearSecurityContext() {
        // given
        TokenClaims claims = new TokenClaims(1L, USER_NICKNAME, List.of("ROLE_USER"));
        HttpServletRequest request = mock(HttpServletRequest.class);

        contextManager.setAuthentication(claims, request);

        // when
        contextManager.clearAuthentication();

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNull();
    }
}
