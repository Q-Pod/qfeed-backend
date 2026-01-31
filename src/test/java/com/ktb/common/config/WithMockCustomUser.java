package com.ktb.common.config;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 테스트에서 인증된 사용자를 시뮬레이션하는 커스텀 어노테이션
 *
 * <p>SecurityContext에 {@link com.ktb.auth.security.adapter.SecurityUserAccount}를 주입하여
 * 실제 인증된 사용자처럼 동작하도록 만듭니다.
 *
 * <p>사용 예시:
 * <pre>
 * @Test
 * @WithMockCustomUser(userId = 1L, email = "test@test.com", nickname = "testUser")
 * void 사용자_조회_성공() {
 *     // 인증된 사용자로 테스트 실행
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {

    /**
     * 사용자 ID (기본값: 1L)
     */
    long userId() default 1L;

    /**
     * 이메일 (기본값: "test@test.com")
     */
    String email() default "test@test.com";

    /**
     * 닉네임 (기본값: "테스트유저")
     */
    String nickname() default "테스트유저";
}
