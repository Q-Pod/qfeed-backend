package com.ktb.auth.dto.jwt;

import java.time.LocalDateTime;

public record RefreshTokenInfo(
        Long id,
        Long familyId,
        boolean used,
        LocalDateTime expiresAt,
        String tokenHash
) {
}
