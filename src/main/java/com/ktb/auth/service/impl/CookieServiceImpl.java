package com.ktb.auth.service.impl;

import com.ktb.auth.dto.CookieSpec;
import com.ktb.auth.jwt.JwtProperties;
import com.ktb.auth.service.CookieService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CookieServiceImpl implements CookieService {

    private static final String COOKIE_PATH = "/api/auth";
    private static final String SAME_SITE_STRICT = "Strict";
    private static final String SAME_SITE_LAX = "Lax";
    private static final int MILLISECONDS_PER_SECOND = 1000;
    private static final int EXPIRED_MAX_AGE = 0;

    private final JwtProperties jwtProperties;

    @Value("${cookie.secure:true}")
    private boolean cookieSecure;

    @Override
    public CookieSpec createRefreshTokenCookie(String refreshToken) {
        int maxAgeSeconds = (int) (jwtProperties.getRefreshTokenExpiration() / MILLISECONDS_PER_SECOND);
        String sameSite = cookieSecure ? SAME_SITE_STRICT : SAME_SITE_LAX;

        return new CookieSpec(
                jwtProperties.getRefreshTokenCookieName(),
                refreshToken,
                maxAgeSeconds,
                COOKIE_PATH,
                true,
                cookieSecure,
                sameSite
        );
    }

    @Override
    public CookieSpec createExpiredRefreshTokenCookie() {
        String sameSite = cookieSecure ? SAME_SITE_STRICT : SAME_SITE_LAX;

        return new CookieSpec(
                jwtProperties.getRefreshTokenCookieName(),
                "",
                EXPIRED_MAX_AGE,
                COOKIE_PATH,
                true,
                cookieSecure,
                sameSite
        );
    }
}
