package com.ktb.auth.controller;

import com.ktb.auth.config.OAuthProperties;
import com.ktb.auth.dto.AuthorizationUrlResult;
import com.ktb.auth.dto.OAuthExchangeCodeResult;
import com.ktb.auth.dto.OAuthLoginResult;
import com.ktb.auth.dto.jwt.TokenRefreshResult;
import com.ktb.auth.dto.request.OAuthExchangeRequest;
import com.ktb.auth.dto.response.LogoutAllResponse;
import com.ktb.auth.dto.response.OAuthLoginResponse;
import com.ktb.auth.dto.response.TokenRefreshResponse;
import com.ktb.auth.security.adapter.SecurityUserAccount;
import com.ktb.auth.service.CookieService;
import com.ktb.auth.service.OAuthApplicationService;
import com.ktb.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final OAuthApplicationService oauthApplicationService;
    private final CookieService cookieService;
    private final OAuthProperties OAuthProperties;

    private static final String PARAM_EXCHANGE_CODE = "exchange_code";
    private static final String PARAM_ERROR = "error";
    private static final String PARAM_ERROR_MESSAGE = "error_message";
    private static final String MESSAGE_OAUTH_EXCHANGE_SUCCESS = "oauth_exchange_success";

    @GetMapping("/oauth/authorization-url")
    @Operation(summary = "OAuth 로그인 시작", description = "OAuth 인증 URL을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 provider",
            content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<Void> getAuthorizationUrl(
        @Parameter(description = "OAuth 제공자", example = "kakao")
        @RequestParam String provider
    ) {
        AuthorizationUrlResult result = oauthApplicationService.getAuthorizationUrl(provider);

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(result.redirectUrl()))
            .build();
    }

    @GetMapping("/oauth/{provider}/callback")
    @Operation(summary = "OAuth 콜백", description = "OAuth 콜백을 처리하고 토큰을 발급합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<Void> handleCallback(
        @Parameter(description = "OAuth Provider name")
        @PathVariable String provider,

        @Parameter(description = "Authorization code")
        @RequestParam String code,

        @Parameter(description = "CSRF state")
        @RequestParam String state,

        HttpServletRequest request
    ) {
        String deviceInfo = extractDeviceInfo(request);
        String clientIp = extractClientIp(request);

        OAuthExchangeCodeResult result = oauthApplicationService.handleCallback(
            provider, code, state, deviceInfo, clientIp
        );

        URI redirectUri = buildRedirectUri(result.exchangeCode(), null, null);

        return ResponseEntity.status(HttpStatus.FOUND)
            .location(redirectUri)
            .build();
    }

    @PostMapping("/oauth/exchange")
    @Operation(summary = "교환 코드로 토큰 발급", description = "OAuth 교환 코드를 사용하여 Access Token과 Refresh Token을 발급합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 발급 성공",
            content = @Content(schema = @Schema(implementation = OAuthLoginResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 교환 코드",
            content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<OAuthLoginResponse>> exchange(
        @Valid @RequestBody OAuthExchangeRequest request,
        HttpServletResponse response
    ) {
        OAuthLoginResult result = oauthApplicationService.exchange(request.exchangeCode());

        response.setHeader("Authorization", "Bearer " + result.accessToken());

        Cookie refreshTokenCookie = cookieService.createRefreshTokenCookie(result.refreshToken());
        response.addCookie(refreshTokenCookie);

        OAuthLoginResponse responseDto = new OAuthLoginResponse(result.user());

        return ResponseEntity.ok(
            new ApiResponse<>(MESSAGE_OAUTH_EXCHANGE_SUCCESS, responseDto)
        );
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 Access Token을 재발급합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "갱신 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Refresh Token 누락",
            content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰",
            content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    @PostMapping("/tokens")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshTokens(
        @CookieValue(value = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        if (refreshToken == null) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>("missing_refresh_token", null));
        }

        TokenRefreshResult result = oauthApplicationService.refreshTokens(refreshToken);

        response.setHeader("Authorization", "Bearer " + result.accessToken());

        Cookie newRefreshTokenCookie = cookieService.createRefreshTokenCookie(result.refreshToken());
        response.addCookie(newRefreshTokenCookie);

        TokenRefreshResponse responseDto = new TokenRefreshResponse(result.expiresIn());

        return ResponseEntity.ok(
            new ApiResponse<TokenRefreshResponse>("token_refresh_success", responseDto)
        );
    }

    @Operation(summary = "로그아웃", description = "단일 기기 로그아웃을 수행합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요",
            content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
        @AuthenticationPrincipal SecurityUserAccount principal,
        @CookieValue(value = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        if (principal == null) {
            return ResponseEntity.status(401)
                .body(new ApiResponse<>("unauthorized_request", null));
        }

        if (refreshToken != null) {
            oauthApplicationService.logout(principal.getAccount().getId(), refreshToken);
        }

        Cookie expiredCookie = cookieService.createExpiredRefreshTokenCookie();
        response.addCookie(expiredCookie);

        return ResponseEntity.ok(
            new ApiResponse<>("logout_success", null)
        );
    }

    @PostMapping("/logout/all")
    @Operation(summary = "전체 로그아웃", description = "모든 기기에서 로그아웃을 수행합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요",
            content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<LogoutAllResponse>> logoutAll(
        @AuthenticationPrincipal SecurityUserAccount principal,
        HttpServletResponse response
    ) {
        if (principal == null) {
            return ResponseEntity.status(401)
                .body(new ApiResponse<>("unauthorized_request", null));
        }

        int revokedCount = oauthApplicationService.logoutAll(principal.getAccount().getId());

        Cookie expiredCookie = cookieService.createExpiredRefreshTokenCookie();
        response.addCookie(expiredCookie);

        return ResponseEntity.ok(
            new ApiResponse<>("all_sessions_logged_out", new LogoutAllResponse(revokedCount))
        );
    }

    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown Device";
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private URI buildRedirectUri(String exchangeCode, String error, String errorMessage) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
            OAuthProperties.getFrontendRedirectUri());

        if (exchangeCode != null) {
            builder.queryParam(PARAM_EXCHANGE_CODE, exchangeCode);
        }
        if (error != null) {
            builder.queryParam(PARAM_ERROR, error);
        }
        if (errorMessage != null) {
            builder.queryParam(PARAM_ERROR_MESSAGE, errorMessage);
        }
        return builder.build(true).toUri();
    }
}
