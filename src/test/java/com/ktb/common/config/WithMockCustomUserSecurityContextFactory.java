package com.ktb.common.config;

import com.ktb.auth.security.adapter.SecurityUserAccount;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * {@link WithMockCustomUser} 어노테이션을 처리하여 SecurityContext를 생성하는 팩토리 클래스
 *
 * <p>테스트 실행 시 SecurityContext에 {@link SecurityUserAccount}를 주입하여
 * 실제 인증된 사용자처럼 동작하도록 만듭니다.
 */
public class WithMockCustomUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        SecurityUserAccount principal = new SecurityUserAccount(
                annotation.userId(), annotation.nickname(), List.of("ROLE_USER")
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        context.setAuthentication(auth);
        return context;
    }
}
