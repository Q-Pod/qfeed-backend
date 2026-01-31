package com.ktb.auth.service;

import com.ktb.auth.domain.RefreshToken;
import com.ktb.auth.domain.RevokeReason;
import com.ktb.auth.domain.TokenFamily;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.dto.jwt.RefreshTokenEntity;
import com.ktb.auth.exception.account.AccountNotFoundException;
import com.ktb.auth.exception.family.FamilyRevokedException;
import com.ktb.auth.exception.family.TokenFamilyNotFoundException;
import com.ktb.auth.exception.token.InvalidRefreshTokenException;
import com.ktb.auth.repository.RefreshTokenRepository;
import com.ktb.auth.repository.TokenFamilyRepository;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.auth.service.impl.RTRServiceImpl;
import com.ktb.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RTRService 단위 테스트 (Edge Cases 포함)")
class RTRServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenFamilyRepository tokenFamilyRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private RTRServiceImpl rtrService;

    private static final Long USER_ID = 1L;
    private static final Long FAMILY_ID = 10L;
    private static final Long TOKEN_ID = 100L;
    private static final String DEVICE_INFO = "Chrome on MacOS";
    private static final String CLIENT_IP = "127.0.0.1";

    @Test
    @DisplayName("Family 생성이 성공해야 한다")
    void createFamily_ShouldSucceed() {
        // given
        UserAccount mockUser = mock(UserAccount.class);
        when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        TokenFamily mockFamily = TokenFamily.create(mockUser, DEVICE_INFO, CLIENT_IP);
        when(tokenFamilyRepository.save(any(TokenFamily.class))).thenReturn(mockFamily);

        // when
        TokenFamily family = rtrService.createFamily(USER_ID, DEVICE_INFO, CLIENT_IP);

        // then
        assertThat(family).isNotNull();
        assertThat(family.getDeviceInfo()).isEqualTo(DEVICE_INFO);
        assertThat(family.getClientIp()).isEqualTo(CLIENT_IP);
        assertThat(family.isRevoked()).isFalse();

        verify(userAccountRepository).findById(USER_ID);
        verify(tokenFamilyRepository).save(any(TokenFamily.class));
    }

    @Test
    @DisplayName("[Edge Case] 존재하지 않는 사용자로 Family 생성 시 예외가 발생해야 한다")
    void createFamily_WithNonExistentUser_ShouldThrowException() {
        // given
        when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> rtrService.createFamily(USER_ID, DEVICE_INFO, CLIENT_IP))
                .isInstanceOf(AccountNotFoundException.class);

        verify(userAccountRepository).findById(USER_ID);
        verify(tokenFamilyRepository, never()).save(any());
    }

    @Test
    @DisplayName("토큰 재사용 탐지 시 Family가 폐기되어야 한다")
    void detectReuse_WithUsedToken_ShouldRevokeFamily() {
        // given
        RefreshTokenEntity usedToken = new RefreshTokenEntity(TOKEN_ID, FAMILY_ID, true, LocalDateTime.now().plusDays(7));

        TokenFamily mockFamily = mock(TokenFamily.class);
        when(tokenFamilyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(mockFamily));

        // when & then
        assertThatThrownBy(() -> rtrService.detectReuse(usedToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("토큰 재사용이 탐지되었습니다");

        verify(mockFamily).revoke(RevokeReason.REUSE_DETECTED);
    }

    @Test
    @DisplayName("사용되지 않은 토큰은 재사용 탐지를 통과해야 한다")
    void detectReuse_WithUnusedToken_ShouldPass() {
        // given
        RefreshTokenEntity unusedToken = new RefreshTokenEntity(TOKEN_ID, FAMILY_ID, false, LocalDateTime.now().plusDays(7));

        // when
        rtrService.detectReuse(unusedToken);

        // then
        verify(tokenFamilyRepository, never()).findById(any());
    }

    @Test
    @DisplayName("토큰 사용 처리가 성공해야 한다")
    void markAsUsed_ShouldSucceed() {
        // given
        RefreshToken mockToken = mock(RefreshToken.class);
        when(refreshTokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(mockToken));

        // when
        rtrService.markAsUsed(TOKEN_ID);

        // then
        verify(mockToken).markAsUsed();
        verify(refreshTokenRepository).findById(TOKEN_ID);
    }

    @Test
    @DisplayName("[Edge Case] 존재하지 않는 RefreshToken 사용 처리 시 예외가 발생해야 한다")
    void markAsUsed_WithNonExistentToken_ShouldThrowException() {
        // given
        when(refreshTokenRepository.findById(TOKEN_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> rtrService.markAsUsed(TOKEN_ID))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("Refresh Token이 유효하지 않습니다");

        verify(refreshTokenRepository).findById(TOKEN_ID);
    }

    @Test
    @DisplayName("Family 활성 상태 확인 - 정상 케이스")
    void validateFamilyActive_WithActiveFamily_ShouldPass() {
        // given
        TokenFamily mockFamily = mock(TokenFamily.class);
        when(mockFamily.isRevoked()).thenReturn(false);
        when(mockFamily.isExpired()).thenReturn(false);
        when(tokenFamilyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(mockFamily));

        // when
        rtrService.validateFamilyActive(FAMILY_ID);

        // then
        verify(tokenFamilyRepository).findById(FAMILY_ID);
        verify(mockFamily).isRevoked();
        verify(mockFamily).isExpired();
    }

    @Test
    @DisplayName("폐기된 Family 확인 시 예외가 발생해야 한다")
    void validateFamilyActive_WithRevokedFamily_ShouldThrowException() {
        // given
        TokenFamily mockFamily = mock(TokenFamily.class);
        when(mockFamily.isRevoked()).thenReturn(true);
        when(tokenFamilyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(mockFamily));

        // when & then
        assertThatThrownBy(() -> rtrService.validateFamilyActive(FAMILY_ID))
                .isInstanceOf(FamilyRevokedException.class);

        verify(tokenFamilyRepository).findById(FAMILY_ID);
    }

    @Test
    @DisplayName("만료된 Family 확인 시 예외가 발생해야 한다")
    void validateFamilyActive_WithExpiredFamily_ShouldThrowException() {
        // given
        TokenFamily mockFamily = mock(TokenFamily.class);
        when(mockFamily.isRevoked()).thenReturn(false);
        when(mockFamily.isExpired()).thenReturn(true);
        when(tokenFamilyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(mockFamily));

        // when & then
        assertThatThrownBy(() -> rtrService.validateFamilyActive(FAMILY_ID))
                .isInstanceOf(FamilyRevokedException.class);

        verify(tokenFamilyRepository).findById(FAMILY_ID);
    }

    @Test
    @DisplayName("[Edge Case] 존재하지 않는 Family 조회 시 예외가 발생해야 한다")
    void validateFamilyActive_WithNonExistentFamily_ShouldThrowException() {
        // given
        when(tokenFamilyRepository.findById(FAMILY_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> rtrService.validateFamilyActive(FAMILY_ID))
                .isInstanceOf(TokenFamilyNotFoundException.class);

        verify(tokenFamilyRepository).findById(FAMILY_ID);
    }

    @Test
    @DisplayName("Family 폐기가 성공해야 한다")
    void revokeFamily_ShouldSucceed() {
        // given
        TokenFamily mockFamily = mock(TokenFamily.class);
        when(tokenFamilyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(mockFamily));

        // when
        rtrService.revokeFamily(FAMILY_ID, RevokeReason.USER_LOGOUT);

        // then
        verify(mockFamily).revoke(RevokeReason.USER_LOGOUT);
        verify(tokenFamilyRepository).findById(FAMILY_ID);
    }

    @Test
    @DisplayName("[Edge Case] 이미 폐기된 Family 재폐기 시도 - 멱등성 확인")
    void revokeFamily_AlreadyRevoked_ShouldBeIdempotent() {
        // given
        TokenFamily mockFamily = mock(TokenFamily.class);
        when(tokenFamilyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(mockFamily));

        // when
        rtrService.revokeFamily(FAMILY_ID, RevokeReason.USER_LOGOUT);
        rtrService.revokeFamily(FAMILY_ID, RevokeReason.USER_LOGOUT);

        // then
        // revoke()가 두 번 호출되어도 예외가 발생하지 않아야 함 (멱등성)
        verify(mockFamily, times(2)).revoke(RevokeReason.USER_LOGOUT);
        verify(tokenFamilyRepository, times(2)).findById(FAMILY_ID);
    }

    @Test
    @DisplayName("[Edge Case] 존재하지 않는 Family 폐기 시도 시 예외가 발생해야 한다")
    void revokeFamily_WithNonExistentFamily_ShouldThrowException() {
        // given
        when(tokenFamilyRepository.findById(FAMILY_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> rtrService.revokeFamily(FAMILY_ID, RevokeReason.USER_LOGOUT))
                .isInstanceOf(TokenFamilyNotFoundException.class);

        verify(tokenFamilyRepository).findById(FAMILY_ID);
    }
}
