package com.ktb.notification.repository;

import com.ktb.notification.domain.UserNotification;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    @Query("SELECT n FROM UserNotification n WHERE n.account.id = :accountId ORDER BY n.createdAt DESC")
    Page<UserNotification> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    @Query("SELECT n FROM UserNotification n WHERE n.account.id = :accountId AND n.read = false ORDER BY n.createdAt DESC")
    List<UserNotification> findUnreadByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT n FROM UserNotification n WHERE n.account.id = :accountId AND n.read = false ORDER BY n.createdAt DESC")
    Page<UserNotification> findUnreadByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM UserNotification n WHERE n.account.id = :accountId AND n.read = false")
    long countUnreadByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT n FROM UserNotification n WHERE n.id = :id AND n.account.id = :accountId")
    Optional<UserNotification> findByIdAndAccountId(@Param("id") Long id, @Param("accountId") Long accountId);

    @Modifying
    @Query("UPDATE UserNotification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP WHERE n.account.id = :accountId AND n.read = false")
    int markAllAsReadByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT n FROM UserNotification n WHERE n.notificationType = :type AND n.referenceId = :referenceId")
    List<UserNotification> findByTypeAndReferenceId(
            @Param("type") NotificationTypeCd type,
            @Param("referenceId") Long referenceId
    );

    boolean existsByAccountIdAndNotificationTypeAndReferenceId(
            Long accountId,
            NotificationTypeCd notificationType,
            Long referenceId
    );

    @Query("SELECT n FROM UserNotification n WHERE n.account.id = :accountId ORDER BY n.createdAt DESC, n.id DESC")
    Slice<UserNotification> findByAccountIdOrderByCreatedAtDescIdDesc(
            @Param("accountId") Long accountId, Pageable pageable);

    @Query("SELECT n FROM UserNotification n WHERE n.account.id = :accountId AND n.id < :cursor ORDER BY n.createdAt DESC, n.id DESC")
    Slice<UserNotification> findByAccountIdAndIdLessThanOrderByIdDesc(
            @Param("accountId") Long accountId,
            @Param("cursor") Long cursor,
            Pageable pageable);
}
