package com.ktb.auth.security.filter;

import com.ktb.auth.security.adapter.SecurityUserAccount;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class LoadTestAuthFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-Load-Test-User-Id";
    private static final String LOAD_TEST_NICKNAME = "부하테스트유저";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String userIdHeader = request.getHeader(USER_ID_HEADER);

        if (userIdHeader != null) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                SecurityUserAccount testUser = new SecurityUserAccount(
                    userId, LOAD_TEST_NICKNAME, List.of("ROLE_USER")
                );
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    testUser, null, testUser.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (NumberFormatException ignored) {
                // 유효하지 않은 userId 헤더 → 인증 없이 통과
            }
        }

        filterChain.doFilter(request, response);
    }
}
