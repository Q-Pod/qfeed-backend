package com.ktb.auth.dto.jwt;

public record RefreshTokenClaims(
        Long userId,
        String userNickname,
        String familyUuid
) {
}

