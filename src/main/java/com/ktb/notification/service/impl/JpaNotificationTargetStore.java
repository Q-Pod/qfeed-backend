package com.ktb.notification.service.impl;

import com.ktb.notification.domain.NotificationTarget;
import com.ktb.notification.domain.enums.NotificationTargetStatusCd;
import com.ktb.notification.repository.NotificationTargetRepository;
import com.ktb.notification.service.NotificationTargetStore;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JpaNotificationTargetStore implements NotificationTargetStore {

    private final NotificationTargetRepository repository;

    @Override
    public NotificationTarget save(NotificationTarget target) {
        return repository.save(target);
    }

    @Override
    public List<NotificationTarget> saveAll(List<NotificationTarget> targets) {
        return repository.saveAll(targets);
    }

    @Override
    public Optional<NotificationTarget> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<NotificationTarget> findByCampaignId(Long campaignId) {
        return repository.findByCampaignId(campaignId);
    }

    @Override
    public Page<NotificationTarget> findByCampaignId(Long campaignId, Pageable pageable) {
        return repository.findByCampaignId(campaignId, pageable);
    }

    @Override
    public List<NotificationTarget> findByCampaignIdAndStatus(Long campaignId, NotificationTargetStatusCd status) {
        return repository.findByCampaignIdAndStatus(campaignId, status);
    }

    @Override
    public List<NotificationTarget> findByAccountId(Long accountId) {
        return repository.findByAccountId(accountId);
    }

    @Override
    public List<NotificationTarget> findByDeviceId(Long deviceId) {
        return repository.findByDeviceId(deviceId);
    }

    @Override
    public Optional<NotificationTarget> findByDedupeKey(String dedupeKey) {
        return repository.findByDedupeKey(dedupeKey);
    }

    @Override
    public boolean existsByDedupeKey(String dedupeKey) {
        return repository.existsByDedupeKey(dedupeKey);
    }

    @Override
    public long countByCampaignIdAndStatus(Long campaignId, NotificationTargetStatusCd status) {
        return repository.countByCampaignIdAndStatus(campaignId, status);
    }

    @Override
    public long countByCampaignId(Long campaignId) {
        return repository.countByCampaignId(campaignId);
    }
}
