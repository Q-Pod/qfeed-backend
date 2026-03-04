package com.ktb.notification.repository;

import com.ktb.notification.domain.UserDevice;
import com.ktb.notification.domain.enums.DevicePlatformCd;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    @Query("SELECT d FROM UserDevice d WHERE d.account.id = :accountId AND d.active = true")
    List<UserDevice> findActiveByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT d FROM UserDevice d WHERE d.account.id = :accountId")
    List<UserDevice> findByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT d FROM UserDevice d WHERE d.account.id = :accountId AND d.platform = :platform AND d.active = true")
    List<UserDevice> findActiveByAccountIdAndPlatform(
            @Param("accountId") Long accountId,
            @Param("platform") DevicePlatformCd platform
    );

    @Query("SELECT COUNT(d) FROM UserDevice d WHERE d.account.id = :accountId AND d.active = true")
    long countActiveByAccountId(@Param("accountId") Long accountId);
}
