package com.ktb.auth.security.adapter;

import com.ktb.auth.dto.jwt.TokenClaims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SpringSecurityContextManager {

    public void setAuthentication(TokenClaims claims, HttpServletRequest request) {
        SecurityUserAccount securityUser = new SecurityUserAccount(claims.userId(), claims.userNickname(), claims.roles());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        securityUser,
                        null,
                        securityUser.getAuthorities()
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Spring Security 인증 컨텍스트 설정: userId={}", claims.userId());
    }

    public void clearAuthentication() {
        SecurityContextHolder.clearContext();
        log.debug("Spring Security 인증 컨텍스트 초기화");
    }
}
