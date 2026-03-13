package com.ktb.auth.service.impl;

import com.ktb.auth.domain.RefreshToken;
import com.ktb.auth.domain.TokenFamily;
import com.ktb.auth.dto.jwt.RefreshTokenInfo;
import com.ktb.auth.exception.family.TokenFamilyNotFoundException;
import com.ktb.auth.exception.token.InvalidRefreshTokenException;
import com.ktb.auth.repository.RefreshTokenRepository;
import com.ktb.auth.repository.TokenFamilyRepository;
import com.ktb.auth.service.RefreshTokenStore;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaRefreshTokenStore implements RefreshTokenStore {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenFamilyRepository tokenFamilyRepository;

    @Override
    @Transactional
    public void save(Long familyId, String tokenHash, LocalDateTime expiresAt) {
        TokenFamily family = tokenFamilyRepository.findById(familyId)
                .orElseThrow(() -> new TokenFamilyNotFoundException(familyId));

        RefreshToken refreshToken = RefreshToken.builder()
                .family(family)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshTokenInfo> findByTokenHash(String tokenHash) {
        return refreshTokenRepository.findByTokenHashWithFamily(tokenHash)
                .map(token -> new RefreshTokenInfo(
                        token.getId(),
                        token.getFamily().getId(),
                        token.getUsed(),
                        token.getExpiresAt(),
                        token.getTokenHash()
                ));
    }

    @Override
    @Transactional
    public void markAsUsed(Long tokenId) {
        RefreshToken token = refreshTokenRepository.findById(tokenId)
                .orElseThrow(InvalidRefreshTokenException::new);
        token.markAsUsed();
    }
}
