package com.ktb.auth.service;

import com.ktb.auth.domain.TokenFamily;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.dto.TokenFamilyInfo;
import com.ktb.auth.exception.account.AccountNotFoundException;
import com.ktb.auth.repository.RefreshTokenRepository;
import com.ktb.auth.repository.TokenFamilyRepository;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.auth.service.impl.RTRServiceImpl;
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
        TokenFamilyInfo familyInfo = rtrService.createFamily(USER_ID, DEVICE_INFO, CLIENT_IP);

        // then
        assertThat(familyInfo).isNotNull();
        assertThat(familyInfo.uuid()).isNotNull();
        assertThat(familyInfo.active()).isTrue();

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
}
