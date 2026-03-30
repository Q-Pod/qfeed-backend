package com.ktb.notification.repository;

import com.ktb.notification.domain.NotificationInbox;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationInboxRepository extends JpaRepository<NotificationInbox, Long> {

    boolean existsByMessageId(String messageId);

    @Modifying
    @Query("DELETE FROM NotificationInbox i WHERE i.createdAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
