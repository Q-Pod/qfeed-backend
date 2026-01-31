package com.ktb.auth.dto.jwt;

import java.util.List;

public record TokenClaims(
        Long userId,
        String userNickname,
        List<String> roles
) {

}
