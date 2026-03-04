package com.ktb.notification.service.impl;

import com.ktb.auth.domain.AccountStatus;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.notification.domain.Campaign;
import com.ktb.notification.domain.Notice;
import com.ktb.notification.domain.NotificationTarget;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.domain.enums.TargetUsersType;
import com.ktb.notification.exception.CampaignNotFoundException;
import com.ktb.notification.exception.NoticeNotFoundException;
import com.ktb.notification.repository.CampaignRepository;
import com.ktb.notification.repository.NoticeRepository;
import com.ktb.notification.repository.NotificationTargetRepository;
import com.ktb.notification.service.CampaignPublishService;
import com.ktb.notification.service.CampaignScheduler;
import com.ktb.notification.service.TargetUserResolver;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignPublishServiceImpl implements CampaignPublishService {

    private static final String NOTICE_DEEPLINK_TEMPLATE = "/notices/%d";
    private static final String NOTICE_DEDUPE_KEY_FORMAT = "NOTICE:%d:%d";

    private final NoticeRepository noticeRepository;
    private final CampaignRepository campaignRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationTargetRepository notificationTargetRepository;
    private final TargetUserResolver targetUserResolver;
    private final CampaignScheduler campaignScheduler;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(Long noticeId, Long campaignId, TargetUsersType targetUsersType, LocalDateTime scheduledAt) {
        Notice notice = noticeRepository.findActiveById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

        List<Long> targetAccountIds = targetUserResolver.resolveTargets(
                NotificationTypeCd.NOTICE, targetUsersType
        );

        if (targetAccountIds.isEmpty()) {
            log.info("No target users for notice {}", noticeId);
            return;
        }

        List<UserAccount> targetAccounts = userAccountRepository.findActiveByIds(
                targetAccountIds, AccountStatus.ACTIVE
        );

        List<NotificationTarget> targets = targetAccounts.stream()
                .map(account -> NotificationTarget.create(
                        account,
                        campaign,
                        notice.getTitle(),
                        notice.getBody(),
                        String.format(NOTICE_DEEPLINK_TEMPLATE, noticeId),
                        String.format(NOTICE_DEDUPE_KEY_FORMAT, noticeId, account.getId()),
                        noticeId
                ))
                .toList();

        notificationTargetRepository.saveAll(targets);

        campaignScheduler.schedule(campaign, scheduledAt);

        log.info("Notice {} published. Campaign: {}, NotificationTargets: {}",
                noticeId, campaignId, targets.size());
    }
}
