package com.ktb.notification.service;

import com.ktb.notification.domain.NotificationTarget;
import com.ktb.notification.domain.enums.NotificationTargetStatusCd;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationTargetStore {

    NotificationTarget save(NotificationTarget target);

    List<NotificationTarget> saveAll(List<NotificationTarget> targets);

    Optional<NotificationTarget> findById(Long id);

    List<NotificationTarget> findByCampaignId(Long campaignId);

    Page<NotificationTarget> findByCampaignId(Long campaignId, Pageable pageable);

    List<NotificationTarget> findByCampaignIdAndStatus(Long campaignId, NotificationTargetStatusCd status);

    List<NotificationTarget> findByAccountId(Long accountId);

    List<NotificationTarget> findByDeviceId(Long deviceId);

    Optional<NotificationTarget> findByDedupeKey(String dedupeKey);

    boolean existsByDedupeKey(String dedupeKey);

    long countByCampaignIdAndStatus(Long campaignId, NotificationTargetStatusCd status);

    long countByCampaignId(Long campaignId);
}
