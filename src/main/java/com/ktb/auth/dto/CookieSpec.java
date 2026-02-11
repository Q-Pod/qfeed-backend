package com.ktb.auth.dto;

import jakarta.servlet.http.Cookie;

public record CookieSpec(
        String name,
        String value,
        int maxAgeSeconds,
        String path,
        boolean httpOnly,
        boolean secure,
        String sameSite
) {

    public Cookie toServletCookie() {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setPath(path);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setAttribute("SameSite", sameSite);
        return cookie;
    }
}
