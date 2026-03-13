package com.ktb.auth.service;

import com.ktb.auth.domain.RefreshToken;
import com.ktb.auth.domain.RevokeReason;
import com.ktb.auth.domain.TokenFamily;
import com.ktb.auth.dto.TokenFamilyInfo;
import com.ktb.auth.exception.family.TokenFamilyNotFoundException;
import com.ktb.auth.repository.RefreshTokenRepository;
import com.ktb.auth.repository.TokenFamilyRepository;
import com.ktb.auth.service.impl.JpaTokenFamilyStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaTokenFamilyStore 단위 테스트")
class JpaTokenFamilyStoreTest {

    @Mock
    private TokenFamilyRepository tokenFamilyRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private JpaTokenFamilyStore jpaStore;

    private static final String FAMILY_UUID = "family-uuid-123";
    private static final String OLD_HASH = "old-hash";
    private static final String NEW_HASH = "new-hash";
    private static final long TTL_MILLIS = 14L * 24 * 60 * 60 * 1000; // 14일

    // ── findByUuid (구 validateFamilyActive 매핑) ──

    @Test
    @DisplayName("활성 Family 조회 시 active=true인 TokenFamilyInfo 반환")
    void findByUuid_WithActiveFamily_ShouldReturnActiveInfo() {
        TokenFamily mockFamily = mock(TokenFamily.class);
        when(mockFamily.getId()).thenReturn(1L);
        when(mockFamily.getUuid()).thenReturn(FAMILY_UUID);
        when(mockFamily.isValid()).thenReturn(true);
        when(tokenFamilyRepository.findByUuid(FAMILY_UUID)).thenReturn(Optional.of(mockFamily));

        Optional<TokenFamilyInfo> result = jpaStore.findByUuid(FAMILY_UUID);

        assertThat(result).isPresent();
        assertThat(result.get().active()).isTrue();
        verify(tokenFamilyRepository).findByUuid(FAMILY_UUID);
    }

    @Test
    @DisplayName("폐기된 Family 조회 시 active=false인 TokenFamilyInfo 반환")
    void findByUuid_WithRevokedFamily_ShouldReturnInactiveInfo() {
        TokenFamily mockFamily = mock(TokenFamily.class);
        when(mockFamily.getId()).thenReturn(1L);
        when(mockFamily.getUuid()).thenReturn(FAMILY_UUID);
        when(mockFamily.isValid()).thenReturn(false);
        when(tokenFamilyRepository.findByUuid(FAMILY_UUID)).thenReturn(Optional.of(mockFamily));

        Optional<TokenFamilyInfo> result = jpaStore.findByUuid(FAMILY_UUID);

        assertThat(result).isPresent();
        assertThat(result.get().active()).isFalse();
    }

    @Test
    @DisplayName("만료된 Family 조회 시 active=false인 TokenFamilyInfo 반환")
    void findByUuid_WithExpiredFamily_ShouldReturnInactiveInfo() {
        TokenFamily mockFamily = mock(TokenFamily.class);
        when(mockFamily.getId()).thenReturn(1L);
        when(mockFamily.getUuid()).thenReturn(FAMILY_UUID);
        when(mockFamily.isValid()).thenReturn(false);
        when(tokenFamilyRepository.findByUuid(FAMILY_UUID)).thenReturn(Optional.of(mockFamily));

        Optional<TokenFamilyInfo> result = jpaStore.findByUuid(FAMILY_UUID);

        assertThat(result).isPresent();
        assertThat(result.get().active()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 Family 조회 시 Optional.empty 반환")
    void findByUuid_WithNonExistentFamily_ShouldReturnEmpty() {
        when(tokenFamilyRepository.findByUuid(FAMILY_UUID)).thenReturn(Optional.empty());

        Optional<TokenFamilyInfo> result = jpaStore.findByUuid(FAMILY_UUID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("정상 토큰 교체 시 기존 토큰 used=true 처리 후 1 반환")
    void rotateFamilyToken_WithFreshToken_ShouldMarkOldTokenAsUsedAndReturn1() {
        TokenFamily mockFamily = mock(TokenFamily.class);
        when(mockFamily.isExpired()).thenReturn(false);
        when(mockFamily.isRevoked()).thenReturn(false);
        when(tokenFamilyRepository.findByUuid(FAMILY_UUID)).thenReturn(Optional.of(mockFamily));

        RefreshToken oldToken = mock(RefreshToken.class);
        when(oldToken.getUsed()).thenReturn(false);
        when(refreshTokenRepository.findByTokenHashWithFamily(OLD_HASH)).thenReturn(Optional.of(oldToken));

        int result = jpaStore.rotateFamilyToken(FAMILY_UUID, OLD_HASH, NEW_HASH, TTL_MILLIS);

        assertThat(result).isEqualTo(1);
        verify(oldToken).markAsUsed();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("이미 사용된 토큰으로 교체 시도 시 Family 폐기 후 -3 반환")
    void rotateFamilyToken_WithReuseDetected_ShouldRevokeFamilyAndReturnReuse() {
        TokenFamily mockFamily = mock(TokenFamily.class);
        when(mockFamily.isExpired()).thenReturn(false);
        when(mockFamily.isRevoked()).thenReturn(false);
        when(tokenFamilyRepository.findByUuid(FAMILY_UUID)).thenReturn(Optional.of(mockFamily));

        RefreshToken usedToken = mock(RefreshToken.class);
        when(usedToken.getUsed()).thenReturn(true);
        when(refreshTokenRepository.findByTokenHashWithFamily(OLD_HASH)).thenReturn(Optional.of(usedToken));

        int result = jpaStore.rotateFamilyToken(FAMILY_UUID, OLD_HASH, NEW_HASH, TTL_MILLIS);

        assertThat(result).isEqualTo(-3);
        verify(mockFamily).revoke(RevokeReason.REUSE_DETECTED);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("[Edge Case] 존재하지 않는 토큰 해시로 교체 시도 시 Family 폐기 후 -3 반환")
    void rotateFamilyToken_WithNonExistentOldToken_ShouldRevokeFamilyAndReturnReuse() {
        TokenFamily mockFamily = mock(TokenFamily.class);
        when(mockFamily.isExpired()).thenReturn(false);
        when(mockFamily.isRevoked()).thenReturn(false);
        when(tokenFamilyRepository.findByUuid(FAMILY_UUID)).thenReturn(Optional.of(mockFamily));

        when(refreshTokenRepository.findByTokenHashWithFamily(OLD_HASH)).thenReturn(Optional.empty());

        int result = jpaStore.rotateFamilyToken(FAMILY_UUID, OLD_HASH, NEW_HASH, TTL_MILLIS);

        assertThat(result).isEqualTo(-3);
        verify(mockFamily).revoke(RevokeReason.REUSE_DETECTED);
    }

    @Test
    @DisplayName("Family 폐기 성공")
    void revokeFamilyState_ShouldSucceed() {
        TokenFamily mockFamily = mock(TokenFamily.class);
        when(tokenFamilyRepository.findByUuid(FAMILY_UUID)).thenReturn(Optional.of(mockFamily));

        jpaStore.revokeFamilyState(FAMILY_UUID, RevokeReason.USER_LOGOUT);

        verify(mockFamily).revoke(RevokeReason.USER_LOGOUT);
    }

    @Test
    @DisplayName("[Edge Case] 존재하지 않는 Family 폐기 시도 시 예외 발생")
    void revokeFamilyState_WithNonExistentFamily_ShouldThrowException() {
        when(tokenFamilyRepository.findByUuid(FAMILY_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jpaStore.revokeFamilyState(FAMILY_UUID, RevokeReason.USER_LOGOUT))
                .isInstanceOf(TokenFamilyNotFoundException.class);

        verify(tokenFamilyRepository).findByUuid(FAMILY_UUID);
    }
}
