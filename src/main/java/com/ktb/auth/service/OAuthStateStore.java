package com.ktb.auth.service;

public interface OAuthStateStore {

    /**
     * State 생성 및 저장
     *
     * @param provider OAuth 제공자 (kakao 등)
     * @return 생성된 state 값
     */
    String generateAndStore(String provider);

    /**
     * State 검증 및 소비 (1회용)
     *
     * @param state 검증할 state 값
     */
    void validateAndConsume(String state);
}