package com.ktb.auth.security.service;

import com.ktb.auth.dto.jwt.TokenClaims;
import com.ktb.auth.security.exception.AuthFailureException;
import com.ktb.common.exception.BusinessException;
import com.ktb.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationService {

    private final TokenService tokenService;

    public Optional<TokenClaims> authenticate(String token) {
        try {
            TokenClaims claims = tokenService.validateAccessToken(token);
            log.debug("JWT 인증 성공: userId={}", claims.userId());
            return Optional.of(claims);
        } catch (BusinessException e) {
            throw new AuthFailureException(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            log.warn("JWT 인증 실패: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
