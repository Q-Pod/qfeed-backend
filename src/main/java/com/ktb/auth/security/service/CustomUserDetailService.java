package com.ktb.auth.security.service;

import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.auth.security.adapter.SecurityUserAccount;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailService implements UserDetailsService {
    private final UserAccountRepository userAccountRepository;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String userId) {
        UserAccount userAccount = userAccountRepository.getReferenceById(Long.valueOf(userId));

        return new SecurityUserAccount(userAccount, List.of("ROLE_USER"));
    }
}
