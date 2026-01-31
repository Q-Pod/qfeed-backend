package com.ktb.auth.service.impl;

import com.ktb.auth.domain.RefreshToken;
import com.ktb.auth.domain.RevokeReason;
import com.ktb.auth.domain.TokenFamily;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.dto.jwt.RefreshTokenEntity;
import com.ktb.auth.exception.account.AccountNotFoundException;
import com.ktb.auth.exception.family.FamilyRevokedException;
import com.ktb.auth.exception.family.TokenFamilyNotFoundException;
import com.ktb.auth.exception.token.InvalidRefreshTokenException;
import com.ktb.auth.exception.token.TokenReuseDetectedException;
import com.ktb.auth.repository.RefreshTokenRepository;
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

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenFamilyRepository tokenFamilyRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    public void detectReuse(RefreshTokenEntity tokenEntity) {
        if (tokenEntity.used()) {
            revokeFamily(tokenEntity.familyId(), RevokeReason.REUSE_DETECTED);
            throw new TokenReuseDetectedException(tokenEntity.familyId());
        }
    }

    @Override
    @Transactional
    public void revokeFamily(Long familyId, RevokeReason reason) {
        TokenFamily family = tokenFamilyRepository.findById(familyId)
                .orElseThrow(() -> new TokenFamilyNotFoundException(familyId));

        family.revoke(reason);
    }

    @Override
    @Transactional
    public void markAsUsed(Long refreshTokenId) {
        RefreshToken token = refreshTokenRepository.findById(refreshTokenId)
                .orElseThrow(() -> new InvalidRefreshTokenException("RefreshToken을 찾을 수 없습니다."));

        token.markAsUsed();
    }

    @Override
    @Transactional
    public TokenFamily createFamily(Long accountId, String deviceInfo, String clientIp) {
        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        TokenFamily family = TokenFamily.create(account, deviceInfo, clientIp);
        return tokenFamilyRepository.save(family);
    }

    @Override
    public void validateFamilyActive(Long familyId) {
        TokenFamily family = tokenFamilyRepository.findById(familyId)
                .orElseThrow(() -> new TokenFamilyNotFoundException(familyId));

        if (family.isRevoked()) {
            throw new FamilyRevokedException();
        }

        if (family.isExpired()) {
            throw new FamilyRevokedException();
        }
    }

}
