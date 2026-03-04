package com.ktb.notification.repository;

import com.ktb.notification.domain.UserNotificationPref;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT p.account.id FROM UserNotificationPref p " +
            "WHERE p.notificationType = :type AND p.enabled = false " +
            "AND p.account.id IN :accountIds")
    List<Long> findDisabledAccountIdsByTypeAndAccountIds(
            @Param("type") NotificationTypeCd type,
            @Param("accountIds") List<Long> accountIds
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    INSERT INTO user_notification_pref (
      account_id,
      noti_type_cd,
      is_enabled,
      created_at,
      updated_at
    )
    SELECT
      ua.account_id,
      t.noti_type_cd,
      true,
      CURRENT_TIMESTAMP,
      CURRENT_TIMESTAMP
    FROM user_account ua
    CROSS JOIN (
      VALUES ('NOTICE'), ('REVISIT'), ('ANSWER_FEEDBACK'), ('PROJECT_REMINDER')
    ) AS t(noti_type_cd)
    LEFT JOIN user_notification_pref p
      ON p.account_id = ua.account_id
     AND p.noti_type_cd = t.noti_type_cd
    WHERE ua.account_status_cd = 'ACTIVE'
      AND ua.deleted_at IS NULL
      AND p.user_noti_pref_id IS NULL
    """, nativeQuery = true)
    void insertDefaultPreferencesForAllActiveUsers();
}
