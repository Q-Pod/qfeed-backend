package com.ktb.auth.dto.jwt;

public record TokenRefreshResult(
        String accessToken,
        String refreshToken,
        int expiresIn
) {
}
