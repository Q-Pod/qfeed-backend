package com.ktb.auth.service.impl;

import com.ktb.auth.domain.TokenFamily;
import com.ktb.auth.dto.TokenFamilyInfo;
import com.ktb.auth.exception.family.TokenFamilyNotFoundException;
import com.ktb.auth.repository.TokenFamilyRepository;
import com.ktb.auth.service.TokenFamilyStore;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaTokenFamilyStore implements TokenFamilyStore {

    private final TokenFamilyRepository tokenFamilyRepository;

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
    public void updateLastUsed(String uuid) {
        tokenFamilyRepository.findByUuid(uuid)
                .ifPresentOrElse(
                    TokenFamily::updateLastUsed,
                        () -> { throw new TokenFamilyNotFoundException(uuid); }
                );
    }
}
