package com.ktb.auth.service;

import com.ktb.auth.dto.TokenFamilyInfo;

import java.util.Optional;

public interface TokenFamilyStore {

    /**
     * UUID로 Token Family 조회
     *
     * @param uuid Family UUID
     * @return TokenFamilyInfo (Optional)
     */
    Optional<TokenFamilyInfo> findByUuid(String uuid);

    /**
     * Family 마지막 사용 시각 업데이트
     *
     * @param uuid Family UUID
     */
    void updateLastUsed(String uuid);
}
