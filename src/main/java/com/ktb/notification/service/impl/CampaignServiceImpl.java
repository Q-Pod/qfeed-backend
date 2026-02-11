package com.ktb.notification.service.impl;

import com.ktb.notification.domain.Campaign;
import com.ktb.notification.dto.request.CampaignCreateRequest;
import com.ktb.notification.dto.response.CampaignResponse;
import com.ktb.notification.exception.CampaignKeyDuplicateException;
import com.ktb.notification.exception.CampaignNotFoundException;
import com.ktb.notification.service.CampaignService;
import com.ktb.notification.service.CampaignStore;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignServiceImpl implements CampaignService {

    private final CampaignStore campaignStore;

    @Override
    @Transactional
    public CampaignResponse createCampaign(CampaignCreateRequest request) {
        if (campaignStore.existsByCampaignKey(request.campaignKey())) {
            throw new CampaignKeyDuplicateException(request.campaignKey());
        }

        Campaign campaign = Campaign.create(
                request.campaignType(),
                request.campaignKey(),
                request.scheduledAt()
        );

        Campaign saved = campaignStore.save(campaign);

        return CampaignResponse.from(saved);
    }

    @Override
    public CampaignResponse getCampaign(Long campaignId) {
        Campaign campaign = campaignStore.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

        return CampaignResponse.from(campaign);
    }

    @Override
    public Page<CampaignResponse> getAllCampaigns(Pageable pageable) {
        return campaignStore.findAllOrderByCreatedAtDesc(pageable)
                .map(CampaignResponse::from);
    }

    @Override
    public List<CampaignResponse> getPendingCampaigns() {
        return campaignStore.findPending().stream()
                .map(CampaignResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public CampaignResponse startCampaign(Long campaignId) {
        return transitionCampaign(campaignId, Campaign::start);
    }

    @Override
    @Transactional
    public CampaignResponse completeCampaign(Long campaignId) {
        return transitionCampaign(campaignId, Campaign::complete);
    }

    @Override
    @Transactional
    public CampaignResponse failCampaign(Long campaignId) {
        return transitionCampaign(campaignId, Campaign::fail);
    }

    @Override
    @Transactional
    public CampaignResponse cancelCampaign(Long campaignId) {
        return transitionCampaign(campaignId, Campaign::cancel);
    }

    private CampaignResponse transitionCampaign(Long campaignId, Consumer<Campaign> transition) {
        Campaign campaign = campaignStore.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

        transition.accept(campaign);

        return CampaignResponse.from(campaign);
    }
}
