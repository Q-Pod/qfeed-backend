package com.ktb.notification.service;

import com.ktb.notification.domain.Campaign;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CampaignStore {

    boolean existsByCampaignKey(String key);

    Campaign save(Campaign campaign);

    Optional<Campaign> findById(Long id);

    Page<Campaign> findAllOrderByCreatedAtDesc(Pageable pageable);

    List<Campaign> findPending();
}
