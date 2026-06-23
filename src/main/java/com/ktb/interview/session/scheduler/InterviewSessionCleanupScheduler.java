package com.ktb.interview.session.scheduler;

import com.ktb.interview.session.service.InterviewSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * InMemory 환경 전용 만료 세션 정리 스케줄러.
 * Redis 환경에서는 TTL이 자동 만료를 처리하므로 비활성화됩니다.
 */
@Slf4j
@Component
@Profile("!redis")
@RequiredArgsConstructor
public class InterviewSessionCleanupScheduler {

    private final InterviewSessionService sessionService;

    @Scheduled(fixedDelayString = "${interview.session.cleanup-interval-ms}")
    public void scheduledCleanup() {
        log.debug("InterviewSessionCleanupScheduler.scheduledCleanup - 만료 세션 정리 시작");
        sessionService.cleanupExpiredSessions();
    }
}