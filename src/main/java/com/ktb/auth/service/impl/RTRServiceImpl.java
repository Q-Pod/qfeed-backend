package com.ktb.auth.service.impl;

import com.ktb.auth.domain.TokenFamily;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.dto.TokenFamilyInfo;
import com.ktb.auth.exception.account.AccountNotFoundException;
import com.ktb.auth.repository.TokenFamilyRepository;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.auth.service.RTRService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RTRServiceImpl implements RTRService {

    private final TokenFamilyRepository tokenFamilyRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional
    public TokenFamilyInfo createFamily(Long accountId, String deviceInfo, String clientIp) {
        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        TokenFamily family = TokenFamily.create(account, deviceInfo, clientIp);
        TokenFamily savedFamily = tokenFamilyRepository.save(family);

        return new TokenFamilyInfo(
                savedFamily.getId(),
                savedFamily.getUuid(),
                savedFamily.isValid()
        );
    }
}
