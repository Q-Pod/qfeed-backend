package com.ktb.auth.jwt;

import com.ktb.auth.exception.token.InvalidAccessTokenException;
import com.ktb.auth.exception.token.InvalidRefreshTokenException;
import com.ktb.auth.exception.token.TokenHashingFailedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_NICKNAME = "nickname";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_FAMILY_UUID = "familyUuid";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";
    private static final String HASH_ALGORITHM = "SHA-256";

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String createAccessToken(Long userId, List<String> roles, String nickname) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_NICKNAME, nickname)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId, String familyUuid, String nickname) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_NICKNAME, nickname)
                .claim(CLAIM_FAMILY_UUID, familyUuid)
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public Claims validateAccessToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidAccessTokenException("Access Token이 비어 있습니다");
        }
        try {
            Claims claims = parseClaims(token);
            String type = claims.get(CLAIM_TYPE, String.class);

            if (!TOKEN_TYPE_ACCESS.equals(type)) {
                throw new InvalidAccessTokenException("Access Token 타입이 아닙니다");
            }

            return claims;
        } catch (ExpiredJwtException e) {
            throw new InvalidAccessTokenException("Access Token이 만료되었습니다");
        } catch (SecurityException | MalformedJwtException e) {
            throw new InvalidAccessTokenException("잘못된 Access Token 서명입니다");
        } catch (UnsupportedJwtException e) {
            throw new InvalidAccessTokenException("지원되지 않는 Access Token입니다");
        } catch (JwtException e) {
            throw new InvalidAccessTokenException("Access Token이 올바르지 않습니다");
        }
    }

    public Claims validateRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh Token이 비어 있습니다");
        }
        try {
            Claims claims = parseClaims(token);
            String type = claims.get(CLAIM_TYPE, String.class);

            if (!TOKEN_TYPE_REFRESH.equals(type)) {
                throw new InvalidRefreshTokenException("Refresh Token 타입이 아닙니다");
            }

            return claims;
        } catch (ExpiredJwtException e) {
            throw new InvalidRefreshTokenException("Refresh Token이 만료되었습니다");
        } catch (SecurityException | MalformedJwtException e) {
            throw new InvalidRefreshTokenException("잘못된 Refresh Token 서명입니다");
        } catch (UnsupportedJwtException e) {
            throw new InvalidRefreshTokenException("지원되지 않는 Refresh Token입니다");
        } catch (JwtException e) {
            throw new InvalidRefreshTokenException("Refresh Token이 올바르지 않습니다");
        }
    }

    public Long getUserIdFromToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                throw new InvalidAccessTokenException("Access Token이 비어 있습니다");
            }
            Claims claims = parseClaims(token);
            return claims.get(CLAIM_USER_ID, Long.class);
        } catch (JwtException e) {
            throw new InvalidAccessTokenException("Access Token 파싱에 실패했습니다");
        }
    }

    public String getFamilyUuidFromToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                throw new InvalidRefreshTokenException("Refresh Token이 비어 있습니다");
            }
            Claims claims = parseClaims(token);
            return claims.get(CLAIM_FAMILY_UUID, String.class);
        } catch (JwtException e) {
            throw new InvalidRefreshTokenException("Refresh Token 파싱에 실패했습니다");
        }
    }

    public int accessTokenExpiresSeconds() {
        Long expiresMs = jwtProperties.getAccessTokenExpiration();
        if (expiresMs == null) {
            return 0;
        }
        return (int) TimeUnit.MILLISECONDS.toSeconds(expiresMs);
    }

    public Duration refreshTokenDuration() {
        Long expiresMs = jwtProperties.getRefreshTokenExpiration();
        if (expiresMs == null) {
            return Duration.ZERO;
        }
        return Duration.ofMillis(expiresMs);
    }

    /**
     * Token Hash 생성 (DB 저장용)
     */
    public String generateTokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new TokenHashingFailedException();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
