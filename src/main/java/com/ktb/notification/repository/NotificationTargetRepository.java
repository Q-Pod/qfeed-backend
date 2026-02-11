package com.ktb.notification.repository;

import com.ktb.notification.domain.NotificationTarget;
import com.ktb.notification.domain.enums.NotificationTargetStatusCd;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationTargetRepository extends JpaRepository<NotificationTarget, Long> {

    @Query("SELECT t FROM NotificationTarget t WHERE t.campaign.id = :campaignId")
    List<NotificationTarget> findByCampaignId(@Param("campaignId") Long campaignId);

    @Query("SELECT t FROM NotificationTarget t WHERE t.campaign.id = :campaignId")
    Page<NotificationTarget> findByCampaignId(@Param("campaignId") Long campaignId, Pageable pageable);

    @Query("SELECT t FROM NotificationTarget t WHERE t.campaign.id = :campaignId AND t.status = :status")
    List<NotificationTarget> findByCampaignIdAndStatus(
            @Param("campaignId") Long campaignId,
            @Param("status") NotificationTargetStatusCd status
    );

    @Query("SELECT t FROM NotificationTarget t WHERE t.account.id = :accountId ORDER BY t.createdAt DESC")
    List<NotificationTarget> findByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT t FROM NotificationTarget t WHERE t.device.id = :deviceId ORDER BY t.createdAt DESC")
    List<NotificationTarget> findByDeviceId(@Param("deviceId") Long deviceId);

    Optional<NotificationTarget> findByDedupeKey(String dedupeKey);

    boolean existsByDedupeKey(String dedupeKey);

    @Query("SELECT COUNT(t) FROM NotificationTarget t WHERE t.campaign.id = :campaignId AND t.status = :status")
    long countByCampaignIdAndStatus(
            @Param("campaignId") Long campaignId,
            @Param("status") NotificationTargetStatusCd status
    );

    @Query("SELECT COUNT(t) FROM NotificationTarget t WHERE t.campaign.id = :campaignId")
    long countByCampaignId(@Param("campaignId") Long campaignId);
}
