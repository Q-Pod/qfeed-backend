package com.ktb.auth.service;

import com.ktb.auth.dto.CookieSpec;

public interface CookieService {

    /**
     * Refresh Token 쿠키 스펙 생성
     *
     * @param refreshToken Refresh Token 값
     * @return 쿠키 스펙
     */
    CookieSpec createRefreshTokenCookie(String refreshToken);

    /**
     * Refresh Token 쿠키 삭제를 위한 만료된 쿠키 스펙 생성
     *
     * @return 만료된 쿠키 스펙
     */
    CookieSpec createExpiredRefreshTokenCookie();
}
