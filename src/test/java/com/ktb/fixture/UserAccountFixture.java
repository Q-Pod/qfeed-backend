package com.ktb.fixture;

import com.ktb.auth.domain.UserAccount;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserAccountFixture {

    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final String DEFAULT_NICKNAME = "테스트유저";
    private static int counter = 0;

    public static UserAccount createUserAccount() {
        return UserAccount.createEmailAccount(
                DEFAULT_EMAIL + (++counter),
                DEFAULT_NICKNAME
        );
    }

    public static UserAccount createUserAccount(String email, String nickname) {
        return UserAccount.createEmailAccount(email, nickname);
    }

    public static UserAccount createUserAccountWithNickname(String nickname) {
        return UserAccount.createEmailAccount(
                DEFAULT_EMAIL + (++counter),
                nickname
        );
    }

    public static UserAccount createMockUserAccount(Long accountId) {
        UserAccount account = mock(UserAccount.class);
        when(account.getId()).thenReturn(accountId);
        when(account.getEmail()).thenReturn(DEFAULT_EMAIL);
        when(account.getNickname()).thenReturn(DEFAULT_NICKNAME);
        when(account.isActive()).thenReturn(true);
        return account;
    }

    public static UserAccount createMockUserAccount(Long accountId, String nickname) {
        UserAccount account = mock(UserAccount.class);
        when(account.getId()).thenReturn(accountId);
        when(account.getEmail()).thenReturn(DEFAULT_EMAIL);
        when(account.getNickname()).thenReturn(nickname);
        when(account.isActive()).thenReturn(true);
        return account;
    }

    public static UserAccount createMockInactiveUserAccount(Long accountId) {
        UserAccount account = mock(UserAccount.class);
        when(account.getId()).thenReturn(accountId);
        when(account.getEmail()).thenReturn(DEFAULT_EMAIL);
        when(account.getNickname()).thenReturn(DEFAULT_NICKNAME);
        when(account.isActive()).thenReturn(false);
        return account;
    }

    public static void resetCounter() {
        counter = 0;
    }
}
