package com.ktb.auth.service.impl;

import com.ktb.auth.dto.jwt.RefreshTokenClaims;
import com.ktb.auth.dto.jwt.RefreshTokenEntity;
import com.ktb.auth.dto.jwt.TokenClaims;
import com.ktb.auth.exception.token.InvalidAccessTokenException;
import com.ktb.auth.exception.token.InvalidRefreshTokenException;
import com.ktb.auth.jwt.JwtProvider;
import com.ktb.auth.repository.RefreshTokenRepository;
import com.ktb.auth.service.TokenService;
import io.jsonwebtoken.Claims;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenServiceImpl implements TokenService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final String DEFAULT_NICKNAME = "사용자";

    @Override
    public String issueAccessToken(Long accountId, List<String> roles, String nickname) {
        if(nickname.isBlank()) {
            nickname = DEFAULT_NICKNAME;
        }

        return jwtProvider.createAccessToken(accountId, roles, nickname);
    }

    @Override
    public TokenClaims validateAccessToken(String accessToken) {
        try {
            Claims claims = jwtProvider.validateAccessToken(accessToken);
            Long userId = claims.get("userId", Long.class);
            String userNickname = claims.get("nickname", String.class);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            return new TokenClaims(userId, userNickname, roles);
        } catch (Exception e) {
            throw new InvalidAccessTokenException();
        }
    }

    @Override
    public RefreshTokenClaims validateRefreshToken(String refreshToken) {
        try {
            Claims claims = jwtProvider.validateRefreshToken(refreshToken);
            Long userId = claims.get("userId", Long.class);
            String userNickname = claims.get("nickname", String.class);
            String familyUuid = claims.get("familyUuid", String.class);

            return new RefreshTokenClaims(userId, userNickname, familyUuid);
        } catch (Exception e) {
            throw new InvalidRefreshTokenException(e.getMessage());
        }
    }

    @Override
    public RefreshTokenEntity findByTokenHash(String tokenHash) {
        return refreshTokenRepository.findByTokenHashWithFamily(tokenHash)
                .map(token -> new RefreshTokenEntity(
                        token.getId(),
                        token.getFamily().getId(),
                        token.getUsed(),
                        token.getExpiresAt()
                ))
                .orElseThrow(InvalidRefreshTokenException::new);
    }
}
