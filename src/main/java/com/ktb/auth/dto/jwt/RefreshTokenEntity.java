package com.ktb.auth.dto.jwt;

import java.time.LocalDateTime;

public record RefreshTokenEntity(
        Long id,
        Long familyId,
        Boolean used,
        LocalDateTime expiresAt) {
}
