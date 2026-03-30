package com.ktb.notification.repository;

import com.ktb.notification.domain.NotificationDeliveryAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryAttemptRepository
        extends JpaRepository<NotificationDeliveryAttempt, Long> {

    List<NotificationDeliveryAttempt> findByNotificationTargetIdOrderByAttemptNoAsc(
            Long notificationTargetId
    );

    int countByNotificationTargetId(Long notificationTargetId);
}
