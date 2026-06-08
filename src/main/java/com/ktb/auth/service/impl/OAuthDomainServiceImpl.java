package com.ktb.auth.service.impl;

import com.ktb.auth.domain.OAuthProvider;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.domain.UserOAuth;
import com.ktb.auth.dto.OAuthExchangePayload;
import com.ktb.auth.dto.response.KakaoUserInfoResponse;
import com.ktb.auth.exception.oauth.OAuthConnectionNotFoundException;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.auth.repository.UserOAuthRepository;
import com.ktb.auth.service.OAuthDomainService;
import com.ktb.auth.service.OAuthExchangeCodeStore;
import com.ktb.auth.service.OAuthStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OAuthDomainServiceImpl implements OAuthDomainService {

    private final UserOAuthRepository userOAuthRepository;
    private final UserAccountRepository userAccountRepository;
    private final OAuthStateStore oAuthStateStore;
    private final OAuthExchangeCodeStore oAuthExchangeCodeStore;

    private static final String DEFAULT_NICKNAME_PREFIX = "사용자";
    private static final int DEFAULT_NICKNAME_SUFFIX_LENGTH = 6;

    @Override
    public String generateAndStoreState(String provider) {
        return oAuthStateStore.generateAndStore(provider);
    }

    @Override
    public void validateAndConsumeState(String state) {
        oAuthStateStore.validateAndConsume(state);
    }

    @Override
    public String generateAndStoreExchangeCode(OAuthExchangePayload payload) {
        return oAuthExchangeCodeStore.generateAndStore(payload);
    }

    @Override
    public OAuthExchangePayload consumeExchangeCode(String exchangeCode) {
        return oAuthExchangeCodeStore.validateAndConsume(exchangeCode);
    }

    @Override
    @Transactional
    public UserAccount findOrCreateAccount(OAuthProvider provider, String providerUserId, KakaoUserInfoResponse userInfo) {
        // 기존 OAuth 연동 조회
        return userOAuthRepository.findByProviderAndProviderUserIdWithAccount(provider, providerUserId)
                .map(UserOAuth::getAccount)
                .orElseGet(() -> createNewAccount(provider, providerUserId, userInfo));
    }

    @Override
    @Transactional
    public void updateOAuthLoginInfo(Long oauthId) {
        UserOAuth userOAuth = userOAuthRepository.findById(oauthId)
                .orElseThrow(() -> new OAuthConnectionNotFoundException(oauthId));

        userOAuth.updateLastLogin();
    }

    /**
     * 신규 계정 생성 (OAuth 최초 로그인)
     */
    private UserAccount createNewAccount(OAuthProvider provider, String providerUserId, KakaoUserInfoResponse userInfo) {
        log.info("신규 계정 생성: provider={}, email={}", provider, userInfo.getEmail());

        String nickname = userInfo.getNickname() != null
                ? userInfo.getNickname()
                : DEFAULT_NICKNAME_PREFIX + providerUserId.substring(
                        0,
                        Math.min(DEFAULT_NICKNAME_SUFFIX_LENGTH, providerUserId.length())
                );

        UserAccount account = UserAccount.createEmailAccount(userInfo.getEmail(), nickname);
        userAccountRepository.save(account);

        UserOAuth accountOAuthInfo = UserOAuth.create(account, provider, providerUserId);
        userOAuthRepository.save(accountOAuthInfo);

        return account;
    }
}
