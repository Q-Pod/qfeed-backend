package com.ktb.auth.service;

import com.ktb.auth.domain.RevokeReason;
import com.ktb.auth.domain.TokenFamily;
import com.ktb.auth.dto.jwt.RefreshTokenEntity;

public interface RTRService {

    /**
     * 토큰 재사용 탐지
     */
    void detectReuse(RefreshTokenEntity tokenEntity);

    /**
     * Family 폐기
     */
    void revokeFamily(Long familyId, RevokeReason reason);

    /**
     * 토큰 사용 처리
     */
    void markAsUsed(Long refreshTokenId);

    /**
     * Family 생성 (새 세션 시작)
     */
    TokenFamily createFamily(Long accountId, String deviceInfo, String clientIp);

    /**
     * Family 활성 상태 확인
     */
    void validateFamilyActive(Long familyId);
}

