package com.ktb.auth.service;

import com.ktb.auth.dto.OAuthExchangePayload;

public interface OAuthExchangeCodeStore {

    /**
     * 교환 코드 생성 및 저장
     *
     * @param payload 교환 코드에 담을 페이로드
     * @return 생성된 교환 코드
     */
    String generateAndStore(OAuthExchangePayload payload);

    /**
     * 교환 코드 검증 및 소비 (1회용)
     *
     * @param exchangeCode 검증할 교환 코드
     * @return 저장된 페이로드
     */
    OAuthExchangePayload validateAndConsume(String exchangeCode);
}