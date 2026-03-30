package com.ktb.notification.repository;

import com.ktb.notification.domain.NotificationOutbox;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    @Query(value = """
        SELECT o FROM NotificationOutbox o
        WHERE o.status = 'PENDING'
          AND o.scheduledAt <= :now
        ORDER BY o.scheduledAt ASC
        LIMIT :limit
        """)
    List<NotificationOutbox> findPendingToRelay(
            @Param("now") Instant now,
            @Param("limit") int limit
    );

    @Modifying
    @Query("""
        UPDATE NotificationOutbox o
        SET o.status = 'PROCESSING', o.lockedAt = :now, o.lockedBy = :instanceId,
            o.updatedAt = :now
        WHERE o.id IN :ids
        """)
    void markProcessingByIds(
            @Param("ids") List<Long> ids,
            @Param("now") Instant now,
            @Param("instanceId") String instanceId
    );

    @Modifying
    @Query("""
        UPDATE NotificationOutbox o
        SET o.status = 'SENT', o.sentAt = :now, o.lockedAt = null,
            o.lockedBy = null, o.updatedAt = :now
        WHERE o.messageId = :messageId
        """)
    void markSentByMessageId(@Param("messageId") String messageId, @Param("now") Instant now);

    @Modifying
    @Query("""
        UPDATE NotificationOutbox o
        SET o.status = 'PENDING', o.lockedAt = null, o.lockedBy = null,
            o.updatedAt = :now
        WHERE o.status = 'PROCESSING'
          AND o.lockedAt < :staleBefore
        """)
    int recoverStaleProcessing(@Param("staleBefore") Instant staleBefore, @Param("now") Instant now);

    @Query("SELECT COUNT(o) FROM NotificationOutbox o WHERE o.status = :status")
    long countByStatus(@Param("status") String status);
}
