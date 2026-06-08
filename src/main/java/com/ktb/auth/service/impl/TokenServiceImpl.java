package com.ktb.auth.service.impl;

import com.ktb.auth.dto.jwt.RefreshTokenClaims;
import com.ktb.auth.dto.jwt.TokenClaims;
import com.ktb.auth.exception.token.InvalidAccessTokenException;
import com.ktb.auth.exception.token.InvalidRefreshTokenException;
import com.ktb.auth.jwt.JwtProvider;
import com.ktb.auth.service.TokenService;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final String DEFAULT_NICKNAME = "사용자";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_NICKNAME = "nickname";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_FAMILY_UUID = "familyUuid";

    private final JwtProvider jwtProvider;

    @Override
    public String issueAccessToken(Long accountId, List<String> roles, String nickname) {
        String effectiveNickname = (nickname == null || nickname.isBlank())
                ? DEFAULT_NICKNAME
                : nickname;

        return jwtProvider.createAccessToken(accountId, roles, effectiveNickname);
    }

    @Override
    public String issueRefreshToken(Long accountId, String familyUuid, String nickname) {
        String effectiveNickname = (nickname == null || nickname.isBlank())
                ? DEFAULT_NICKNAME
                : nickname;

        return jwtProvider.createRefreshToken(accountId, familyUuid, effectiveNickname);
    }

    @Override
    public TokenClaims validateAccessToken(String accessToken) {
        try {
            Claims claims = jwtProvider.validateAccessToken(accessToken);
            Long userId = claims.get(CLAIM_USER_ID, Long.class);
            String userNickname = claims.get(CLAIM_NICKNAME, String.class);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get(CLAIM_ROLES, List.class);

            return new TokenClaims(userId, userNickname, roles);
        } catch (Exception e) {
            throw new InvalidAccessTokenException();
        }
    }

    @Override
    public RefreshTokenClaims validateRefreshToken(String refreshToken) {
        try {
            Claims claims = jwtProvider.validateRefreshToken(refreshToken);
            Long userId = claims.get(CLAIM_USER_ID, Long.class);
            String userNickname = claims.get(CLAIM_NICKNAME, String.class);
            String familyUuid = claims.get(CLAIM_FAMILY_UUID, String.class);

            return new RefreshTokenClaims(userId, userNickname, familyUuid);
        } catch (Exception e) {
            throw new InvalidRefreshTokenException();
        }
    }

    @Override
    public String generateTokenHash(String token) {
        return jwtProvider.generateTokenHash(token);
    }

    @Override
    public Duration getRefreshTokenDuration() {
        return jwtProvider.refreshTokenDuration();
    }

    @Override
    public long getAccessTokenExpiresSeconds() {
        return jwtProvider.accessTokenExpiresSeconds();
    }
}
