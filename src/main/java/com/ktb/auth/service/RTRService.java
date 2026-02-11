package com.ktb.auth.service;

import com.ktb.auth.domain.RevokeReason;
import com.ktb.auth.dto.TokenFamilyInfo;
import com.ktb.auth.dto.jwt.RefreshTokenInfo;

public interface RTRService {

    /**
     * 토큰 재사용 탐지
     */
    void detectReuse(RefreshTokenInfo tokenInfo);

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
    TokenFamilyInfo createFamily(Long accountId, String deviceInfo, String clientIp);

    /**
     * Family 활성 상태 확인
     */
    void validateFamilyActive(Long familyId);

    /**
     * 계정의 모든 Family 폐기 (전체 로그아웃)
     *
     * @param accountId 계정 ID
     * @param reason    폐기 사유
     * @return 폐기된 Family 수
     */
    int revokeAllFamilies(Long accountId, RevokeReason reason);
}

