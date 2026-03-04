package com.ktb.notification.service.impl;

import com.ktb.notification.domain.Campaign;
import com.ktb.notification.domain.Notice;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.dto.request.NoticePublishRequest;
import com.ktb.notification.dto.response.NoticePublishResult;
import com.ktb.notification.event.NoticePublishedEvent;
import com.ktb.notification.exception.CampaignKeyDuplicateException;
import com.ktb.notification.exception.NoticeNotFoundException;
import com.ktb.notification.repository.CampaignRepository;
import com.ktb.notification.repository.NoticeRepository;
import com.ktb.notification.service.NoticePublishApplicationService;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticePublishApplicationServiceImpl implements NoticePublishApplicationService {

    private static final String CAMPAIGN_KEY_FORMAT = "NOTICE:%d";

    private final NoticeRepository noticeRepository;
    private final CampaignRepository campaignRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public NoticePublishResult publish(Long noticeId, NoticePublishRequest request) {
        LocalDateTime scheduledAt = (
            request.scheduledAt() != null
                ? request.scheduledAt() : OffsetDateTime.now()
        ).withOffsetSameInstant(ZoneOffset.UTC)
            .toLocalDateTime();

        Notice notice = noticeRepository.findActiveById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));

        notice.publish();

        Campaign campaign = createCampaign(noticeId, scheduledAt);

        eventPublisher.publishEvent(new NoticePublishedEvent(
                this, noticeId, campaign.getId(), request.targetUsers(), scheduledAt
        ));

        log.info("Notice {} published. Campaign: {} scheduled at {}",
                noticeId, campaign.getId(), scheduledAt);

        return NoticePublishResult.scheduled(noticeId, campaign.getId());
    }

    private Campaign createCampaign(Long noticeId, LocalDateTime scheduledAt) {
        String campaignKey = generateCampaignKey(noticeId);
        Campaign campaign = Campaign.create(NotificationTypeCd.NOTICE, campaignKey, scheduledAt);
        try {
            return campaignRepository.save(campaign);
        } catch (DataIntegrityViolationException e) {
            throw new CampaignKeyDuplicateException(campaignKey);
        }
    }

    private String generateCampaignKey(Long noticeId) {
        return String.format(CAMPAIGN_KEY_FORMAT, noticeId);
    }
}
