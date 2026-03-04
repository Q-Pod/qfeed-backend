package com.ktb.auth.repository;

import com.ktb.auth.domain.AccountStatus;
import com.ktb.auth.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT a.id FROM UserAccount a WHERE a.status = :status AND a.deletedAt IS NULL")
    List<Long> findAllActiveAccountIds(@Param("status") AccountStatus status);

    @Query("SELECT a FROM UserAccount a WHERE a.id IN :ids AND a.status = :status AND a.deletedAt IS NULL")
    List<UserAccount> findActiveByIds(@Param("ids") List<Long> ids, @Param("status") AccountStatus status);
}
