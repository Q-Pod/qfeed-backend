package com.ktb.notification.service;

import com.ktb.notification.dto.request.CampaignCreateRequest;
import com.ktb.notification.dto.response.CampaignResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CampaignService {

    CampaignResponse createCampaign(CampaignCreateRequest request);

    CampaignResponse getCampaign(Long campaignId);

    Page<CampaignResponse> getAllCampaigns(Pageable pageable);

    List<CampaignResponse> getPendingCampaigns();

    CampaignResponse startCampaign(Long campaignId);

    CampaignResponse completeCampaign(Long campaignId);

    CampaignResponse failCampaign(Long campaignId);

    CampaignResponse cancelCampaign(Long campaignId);
}
