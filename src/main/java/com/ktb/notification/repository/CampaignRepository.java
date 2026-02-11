package com.ktb.notification.repository;

import com.ktb.notification.domain.Campaign;
import com.ktb.notification.domain.enums.CampaignStatusCd;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Optional<Campaign> findByCampaignKey(String campaignKey);

    boolean existsByCampaignKey(String campaignKey);

    @Query("SELECT c FROM Campaign c WHERE c.status = :status ORDER BY c.scheduledAt ASC")
    List<Campaign> findByStatus(@Param("status") CampaignStatusCd status);

    @Query("SELECT c FROM Campaign c WHERE c.status = :status")
    Page<Campaign> findByStatus(@Param("status") CampaignStatusCd status, Pageable pageable);

    @Query("SELECT c FROM Campaign c WHERE c.campaignType = :type ORDER BY c.createdAt DESC")
    List<Campaign> findByType(@Param("type") NotificationTypeCd type);

    @Query("SELECT c FROM Campaign c WHERE c.status = 'READY' AND c.scheduledAt <= :now ORDER BY c.scheduledAt ASC")
    List<Campaign> findReadyToStart(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Campaign c WHERE c.status IN ('READY', 'RUNNING') ORDER BY c.scheduledAt ASC")
    List<Campaign> findPending();

    @Query("SELECT c FROM Campaign c ORDER BY c.createdAt DESC")
    Page<Campaign> findAllOrderByCreatedAtDesc(Pageable pageable);
}
