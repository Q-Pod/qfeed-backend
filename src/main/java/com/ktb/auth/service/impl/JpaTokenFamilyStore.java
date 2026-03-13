package com.ktb.auth.service.impl;

import com.ktb.auth.domain.RefreshToken;
import com.ktb.auth.domain.RevokeReason;
import com.ktb.auth.domain.TokenFamily;
import com.ktb.auth.dto.TokenFamilyInfo;
import com.ktb.auth.exception.family.TokenFamilyNotFoundException;
import com.ktb.auth.repository.RefreshTokenRepository;
import com.ktb.auth.repository.TokenFamilyRepository;
import com.ktb.auth.service.TokenFamilyStore;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaTokenFamilyStore implements TokenFamilyStore {

    private final TokenFamilyRepository tokenFamilyRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public Optional<TokenFamilyInfo> findByUuid(String uuid) {
        return tokenFamilyRepository.findByUuid(uuid)
                .map(family -> new TokenFamilyInfo(
                        family.getId(),
                        family.getUuid(),
                        family.isValid()
                ));
    }

    @Override
    @Transactional
    public void initFamilyState(String familyUuid, String tokenHash, long ttlMillis) {
        TokenFamily family = tokenFamilyRepository.findByUuid(familyUuid)
                .orElseThrow(() -> new TokenFamilyNotFoundException(familyUuid));

        LocalDateTime expiresAt = LocalDateTime.now().plus(Duration.ofMillis(ttlMillis));
        RefreshToken token = RefreshToken.builder()
                .family(family)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(token);
    }

    @Override
    @Transactional
    public int rotateFamilyToken(String familyUuid, String oldHash, String newHash, long ttlMillis) {
        TokenFamily family = tokenFamilyRepository.findByUuid(familyUuid).orElse(null);
        if (family == null || family.isExpired()) {
            return -1;
        }
        if (family.isRevoked()) {
            return -2;
        }

        RefreshToken oldToken = refreshTokenRepository.findByTokenHashWithFamily(oldHash).orElse(null);
        if (oldToken == null || oldToken.getUsed()) {
            family.revoke(RevokeReason.REUSE_DETECTED);
            return -3;
        }

        oldToken.markAsUsed();
        LocalDateTime expiresAt = LocalDateTime.now().plus(Duration.ofMillis(ttlMillis));
        RefreshToken newToken = RefreshToken.builder()
                .family(family)
                .tokenHash(newHash)
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(newToken);
        family.updateLastUsed();
        return 1;
    }

    @Override
    @Transactional
    public void revokeFamilyState(String familyUuid, RevokeReason reason) {
        TokenFamily family = tokenFamilyRepository.findByUuid(familyUuid)
                .orElseThrow(() -> new TokenFamilyNotFoundException(familyUuid));
        family.revoke(reason);
    }

    @Override
    @Transactional
    public int revokeAllFamilyStates(Long accountId, RevokeReason reason) {
        return tokenFamilyRepository.revokeAllByAccountId(accountId, reason);
    }

    public List<String> findActiveUuids(Long accountId) {
        return tokenFamilyRepository.findActiveUuidsByAccountId(accountId);
    }
}
