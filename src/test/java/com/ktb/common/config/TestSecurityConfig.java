package com.ktb.common.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 테스트 전용 Security 설정
 *
 * <p>모든 테스트에서 Security 관련 설정을 간소화하고,
 * 필요한 빈들을 자동으로 제공합니다.
 *
 * <p>사용 방법:
 * <pre>
 * @WebMvcTest
 * @Import(TestSecurityConfig.class)
 * class MyControllerTest {
 *     // 테스트 코드
 * }
 * </pre>
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {
    /**
     * 테스트용 Security Filter Chain
     * - CSRF 비활성화
     * - 모든 요청 허용 (permitAll)
     */
    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
