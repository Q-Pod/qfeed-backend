package com.ktb.notification.service.impl;

import com.ktb.notification.domain.Campaign;
import com.ktb.notification.repository.CampaignRepository;
import com.ktb.notification.service.CampaignStore;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JpaCampaignStore implements CampaignStore {

    private final CampaignRepository repository;

    @Override
    public boolean existsByCampaignKey(String key) {
        return repository.existsByCampaignKey(key);
    }

    @Override
    public Campaign save(Campaign campaign) {
        return repository.save(campaign);
    }

    @Override
    public Optional<Campaign> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Page<Campaign> findAllOrderByCreatedAtDesc(Pageable pageable) {
        return repository.findAllOrderByCreatedAtDesc(pageable);
    }

    @Override
    public List<Campaign> findPending() {
        return repository.findPending();
    }
}
