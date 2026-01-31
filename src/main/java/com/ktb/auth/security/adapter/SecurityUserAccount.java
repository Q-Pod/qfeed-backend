package com.ktb.auth.security.adapter;

import java.util.List;

import com.ktb.auth.domain.UserAccount;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

@Getter
public class SecurityUserAccount extends User {

    private final UserAccount account;

    public SecurityUserAccount(UserAccount account, List<String> roles) {
        super(
                account.getNickname(),
                "",
                roles.stream().map(SimpleGrantedAuthority::new).toList()
        );
        this.account = account;
    }

    public SecurityUserAccount(Long userId, String nickname, List<String> roles) {
        super(
                nickname,
                "",
                roles.stream().map(SimpleGrantedAuthority::new).toList()
        );
        this.account = UserAccount.createIdAndNicknameAccount(userId, nickname);
    }
}
