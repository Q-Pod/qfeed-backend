package com.ktb.notification.repository;

import com.ktb.notification.domain.UserNotificationPref;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNotificationPrefRepository extends JpaRepository<UserNotificationPref, Long> {

    @Query("SELECT p FROM UserNotificationPref p WHERE p.account.id = :accountId")
    List<UserNotificationPref> findAllByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT p FROM UserNotificationPref p WHERE p.account.id = :accountId AND p.notificationType = :type")
    Optional<UserNotificationPref> findByAccountIdAndNotificationType(
            @Param("accountId") Long accountId,
            @Param("type") NotificationTypeCd type
    );

    @Query("SELECT p FROM UserNotificationPref p WHERE p.account.id = :accountId AND p.enabled = true")
    List<UserNotificationPref> findEnabledByAccountId(@Param("accountId") Long accountId);

    boolean existsByAccountIdAndNotificationType(Long accountId, NotificationTypeCd notificationType);
}
