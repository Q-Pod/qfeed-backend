package com.ktb.notification.worker;

import com.ktb.async.contract.FeedbackCompletedEvent;
import com.ktb.async.contract.NotificationRequestedEvent;
import com.ktb.async.contract.NotificationType;
import com.ktb.async.contract.Priority;
import com.ktb.async.core.worker.AbstractEventWorker;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.service.UserAccountService;
import com.ktb.notification.domain.NotificationTarget;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.repository.NotificationTargetRepository;
import com.ktb.notification.service.UserNotificationService;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationWorker extends AbstractEventWorker<NotificationRequestedEvent> {

    private static final String ANSWER_FEEDBACK_COMPLETED_CODE = "answer_feedback_completed";
    private static final String ANSWER_FEEDBACK_TITLE = "AI 피드백이 도착했습니다";
    private static final String ANSWER_FEEDBACK_BODY = "제출한 답변의 AI 피드백 생성이 완료되었습니다.";
    private static final String ANSWER_FEEDBACK_DEEPLINK_TEMPLATE = "/answers/%d";
    private static final String DEDUPE_KEY_FORMAT = "%s:%s:%d";

    private final UserNotificationService userNotificationService;
    private final NotificationTargetRepository notificationTargetRepository;
    private final UserAccountService userAccountService;

    @Async("notificationWorkerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onFeedbackCompleted(FeedbackCompletedEvent event) {
        handle(toNotificationRequest(event));
    }

    @Async("notificationWorkerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationRequested(NotificationRequestedEvent event) {
        handle(event);
    }

    @Override
    @Transactional
    protected void process(NotificationRequestedEvent event) {
        NotificationTypeCd type = resolveNotificationType(event.notificationCode());
        Long referenceId = tryParseLong(event.correlationId());
        String dedupeKey = buildDedupeKey(event.notificationCode(), referenceId, event.targetAccountId());

        Optional<NotificationTarget> existing = notificationTargetRepository.findByDedupeKey(dedupeKey);
        if (existing.isPresent() && existing.get().isSent()) {
            log.info("Already delivered - dedupeKey={}", dedupeKey);
            return;
        }

        String title = event.templateParams().getOrDefault("title", "");
        String body = event.templateParams().getOrDefault("body", "");

        NotificationTarget target = existing.orElseGet(() -> {
            UserAccount account = userAccountService.findById(event.targetAccountId());
            return notificationTargetRepository.save(NotificationTarget.create(
                    account, null, title, body, event.deepLink(), dedupeKey, referenceId
            ));
        });

        target.markAsQueued();
        userNotificationService.createNotification(
                event.targetAccountId(), type, title, body, event.deepLink(), referenceId);
        target.markAsSent();
    }

    private NotificationRequestedEvent toNotificationRequest(FeedbackCompletedEvent event) {
        return NotificationRequestedEvent.create(
                "notification-worker",
                event.traceId(),
                event.accountId(),
                NotificationType.IN_APP,
                ANSWER_FEEDBACK_COMPLETED_CODE,
                Map.of("title", ANSWER_FEEDBACK_TITLE, "body", ANSWER_FEEDBACK_BODY),
                String.format(ANSWER_FEEDBACK_DEEPLINK_TEMPLATE, event.answerId()),
                Priority.NORMAL,
                String.valueOf(event.answerId())
        );
    }

    private String buildDedupeKey(String notificationCode, Long referenceId, Long accountId) {
        return String.format(DEDUPE_KEY_FORMAT, notificationCode, referenceId, accountId);
    }

    private NotificationTypeCd resolveNotificationType(String code) {
        if (code.equals(ANSWER_FEEDBACK_COMPLETED_CODE)) {
            return NotificationTypeCd.ANSWER_FEEDBACK;
        }
        throw new IllegalArgumentException("Unsupported notification code: " + code);
    }

    private Long tryParseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
