package com.ktb.auth.service;

import com.ktb.auth.dto.TokenFamilyInfo;

public interface RTRService {

    /**
     * Family 생성 (새 세션 시작 - 로그인 시)
     */
    TokenFamilyInfo createFamily(Long accountId, String deviceInfo, String clientIp);
}
