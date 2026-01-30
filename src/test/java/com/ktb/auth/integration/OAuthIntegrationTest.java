package com.ktb.auth.integration;

import com.ktb.auth.client.KakaoOAuth2Client;
import com.ktb.file.service.impl.S3PresignedUrlServiceImpl;
import com.ktb.auth.domain.TokenFamily;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.dto.KakaoAccount;
import com.ktb.auth.dto.KakaoProfile;
import com.ktb.auth.dto.response.KakaoUserInfoResponse;
import com.ktb.auth.repository.RefreshTokenRepository;
import com.ktb.auth.repository.TokenFamilyRepository;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.auth.repository.UserOAuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("OAuth 통합 테스트")
class OAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserOAuthRepository userOAuthRepository;

    @Autowired
    private TokenFamilyRepository tokenFamilyRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private KakaoOAuth2Client kakaoOAuth2Client;

    @MockitoBean
    private S3PresignedUrlServiceImpl s3PresignedUrlServiceImpl;

    private static final String AUTHORIZATION_CODE = "auth-code-123";
    private static final String KAKAO_ACCESS_TOKEN = "kakao.access.token";
    private static final long KAKAO_USER_ID = 12345L;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        tokenFamilyRepository.deleteAll();
        userOAuthRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    @DisplayName("전체 OAuth 로그인 플로우 - 신규 사용자")
    void fullOAuthLoginFlow_WithNewUser() throws Exception {
        // given
        KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse(
                KAKAO_USER_ID,
                new KakaoAccount("newuser@example.com", new KakaoProfile("신규유저", null))
        );

        when(kakaoOAuth2Client.getAccessToken(anyString())).thenReturn(KAKAO_ACCESS_TOKEN);
        when(kakaoOAuth2Client.getUserInfo(KAKAO_ACCESS_TOKEN)).thenReturn(userInfo);

        // Step 1: Authorization URL 생성 → 302 리다이렉트
        MvcResult urlResult = mockMvc.perform(get("/api/auth/oauth/authorization-url")
                        .param("provider", "kakao"))
                .andExpect(status().isFound())
                .andExpect(header().exists("Location"))
                .andReturn();

        String state = extractParam(urlResult.getResponse().getHeader("Location"), "state");
        assertThat(state).isNotBlank();

        // Step 2: OAuth 콜백 → 302 리다이렉트 (exchange_code 포함)
        MvcResult callbackResult = mockMvc.perform(get("/api/auth/oauth/kakao/callback")
                        .param("code", AUTHORIZATION_CODE)
                        .param("state", state))
                .andExpect(status().isFound())
                .andExpect(header().exists("Location"))
                .andReturn();

        String exchangeCode = extractParam(
                callbackResult.getResponse().getHeader("Location"), "exchange_code");
        assertThat(exchangeCode).isNotBlank();

        // Step 3: Exchange 코드로 토큰 발급 → 200
        String exchangeRequestBody = "{\"exchangeCode\":\"" + exchangeCode + "\"}";
        MvcResult exchangeResult = mockMvc.perform(post("/api/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exchangeRequestBody))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andExpect(jsonPath("$.data.user.nickname").value("신규유저"))
                .andExpect(jsonPath("$.data.user.isNewUser").value(true))
                .andExpect(cookie().exists("refreshToken"))
                .andReturn();

        Cookie refreshTokenCookie = exchangeResult.getResponse().getCookie("refreshToken");
        assertThat(refreshTokenCookie).isNotNull();
        assertThat(refreshTokenCookie.isHttpOnly()).isTrue();
        assertThat(refreshTokenCookie.getSecure()).isTrue();

        // DB 검증
        assertThat(userAccountRepository.count()).isEqualTo(1);
        assertThat(userOAuthRepository.count()).isEqualTo(1);
        assertThat(tokenFamilyRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);

        UserAccount savedUser = userAccountRepository.findAll().getFirst();
        assertThat(savedUser.getNickname()).isEqualTo("신규유저");
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
    }

    @Test
    @DisplayName("RTR (Refresh Token Rotation) 플로우")
    void refreshTokenRotationFlow() throws Exception {
        // given
        LoginResult loginResult = performFullLogin();
        String oldRefreshToken = loginResult.refreshToken();

        // 1. Refresh Token 사용 → 새 토큰 발급
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/tokens")
                        .cookie(new Cookie("refreshToken", oldRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andExpect(jsonPath("$.data.expiresIn").exists())
                .andExpect(cookie().exists("refreshToken"))
                .andReturn();

        String newRefreshToken = extractRefreshTokenFromCookie(refreshResult);
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

        // 2. 이전 Refresh Token 재사용 시도 → Token Family 폐기
        mockMvc.perform(post("/api/auth/tokens")
                        .cookie(new Cookie("refreshToken", oldRefreshToken)))
                .andExpect(status().isForbidden());

        // 3. 새 Refresh Token도 사용 불가 (Family 폐기됨)
        mockMvc.perform(post("/api/auth/tokens")
                        .cookie(new Cookie("refreshToken", newRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("다중 기기 세션 관리")
    void multiDeviceSessionManagement() throws Exception {
        // given: 2개 기기에서 로그인
        LoginResult device1 = performFullLogin();
        LoginResult device2 = performFullLogin();

        // 각 기기에서 Token Refresh 성공 확인
        mockMvc.perform(post("/api/auth/tokens")
                        .cookie(new Cookie("refreshToken", device1.refreshToken())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/tokens")
                        .cookie(new Cookie("refreshToken", device2.refreshToken())))
                .andExpect(status().isOk());

        assertThat(tokenFamilyRepository.count()).isEqualTo(2);

        mockMvc.perform(post("/api/auth/logout/all")
                        .header("Authorization", "Bearer " + device1.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revokedSessionsCount").value(2));

        // 모든 세션 무효화 확인
        assertThat(tokenFamilyRepository.findAll())
                .allMatch(TokenFamily::isRevoked);
    }

    @Test
    @DisplayName("보안 시나리오 - Refresh Token 탈취 후 재사용 시도")
    void securityScenario_RefreshTokenTheftAndReuse() throws Exception {
        // given
        LoginResult loginResult = performFullLogin();
        String stolenRefreshToken = loginResult.refreshToken();

        // 1. 정상 사용자가 Token Refresh
        MvcResult legitimateRefresh = mockMvc.perform(post("/api/auth/tokens")
                        .cookie(new Cookie("refreshToken", stolenRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        String newRefreshToken = extractRefreshTokenFromCookie(legitimateRefresh);

        // 2. 공격자가 탈취한 토큰 재사용 시도 → Family 폐기
        mockMvc.perform(post("/api/auth/tokens")
                        .cookie(new Cookie("refreshToken", stolenRefreshToken)))
                .andExpect(status().isForbidden());

        // 3. 정상 사용자의 새 토큰도 무효화됨 (보안을 위한 Family 폐기)
        mockMvc.perform(post("/api/auth/tokens")
                        .cookie(new Cookie("refreshToken", newRefreshToken)))
                .andExpect(status().isUnauthorized());

        // 4. Family가 폐기되었는지 확인
        assertThat(tokenFamilyRepository.findAll())
                .allMatch(TokenFamily::isRevoked);
    }

    private record LoginResult(String accessToken, String refreshToken) {}

    private LoginResult performFullLogin() throws Exception {
        KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse(
                KAKAO_USER_ID,
                new KakaoAccount("user@example.com", new KakaoProfile("유저", null))
        );

        when(kakaoOAuth2Client.getAccessToken(anyString())).thenReturn(KAKAO_ACCESS_TOKEN);
        when(kakaoOAuth2Client.getUserInfo(anyString())).thenReturn(userInfo);

        // Step 1: Authorization URL → state 추출
        MvcResult urlResult = mockMvc.perform(get("/api/auth/oauth/authorization-url")
                        .param("provider", "kakao"))
                .andExpect(status().isFound())
                .andReturn();

        String state = extractParam(urlResult.getResponse().getHeader("Location"), "state");

        // Step 2: Callback → exchange_code 추출
        MvcResult callbackResult = mockMvc.perform(get("/api/auth/oauth/kakao/callback")
                        .param("code", AUTHORIZATION_CODE)
                        .param("state", state))
                .andExpect(status().isFound())
                .andReturn();

        String exchangeCode = extractParam(
                callbackResult.getResponse().getHeader("Location"), "exchange_code");

        // Step 3: Exchange → accessToken + refreshToken
        String exchangeRequestBody = "{\"exchangeCode\":\"" + exchangeCode + "\"}";
        MvcResult exchangeResult = mockMvc.perform(post("/api/auth/oauth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exchangeRequestBody))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = exchangeResult.getResponse().getHeader("Authorization");
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        String refreshToken = extractRefreshTokenFromCookie(exchangeResult);

        return new LoginResult(accessToken, refreshToken);
    }

    private String extractParam(String url, String paramName) {
        URI uri = URI.create(url);
        String query = uri.getQuery();
        if (query == null) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && pair[0].equals(paramName)) {
                return pair[1];
            }
        }
        return null;
    }

    private String extractRefreshTokenFromCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("refreshToken");
        return cookie != null ? cookie.getValue() : null;
    }
}
