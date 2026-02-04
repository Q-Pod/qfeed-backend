package com.ktb.common.util;

import jakarta.servlet.http.HttpServletRequest;

public final class HttpRequestUtils {

    public static String extractClientIp(HttpServletRequest request, String forwardedForHeader, String realIpHeader) {
        String xForwardedFor = request.getHeader(forwardedForHeader);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader(realIpHeader);
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
