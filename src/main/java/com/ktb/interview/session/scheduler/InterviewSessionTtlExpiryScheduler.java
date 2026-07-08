package com.ktb.interview.session.scheduler;

import com.ktb.interview.session.config.InterviewSessionStorePolicy;
import com.ktb.interview.session.metrics.InterviewSessionMetrics;
import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@Profile("redis")
@RequiredArgsConstructor
public class InterviewSessionTtlExpiryScheduler {

    private final StringRedisTemplate stringRedisTemplate;
    private final InterviewSessionStorePolicy storePolicy;
    private final InterviewSessionMetrics metrics;

    @Scheduled(fixedDelayString = "${interview.session.ttl-check-interval-ms:30000}")
    @SchedulerLock(name = "interviewSessionTtlCheck", lockAtMostFor = "PT50S", lockAtLeastFor = "PT5S")
    public void detectExpiredSessions() {
        long now = Instant.now().getEpochSecond();
        String indexKey = "interview:session:expiry-index";

        Set<String> sessionIds = stringRedisTemplate.opsForZSet().rangeByScore(indexKey, 0, now);
        if (sessionIds == null || sessionIds.isEmpty()) return;

        List<String> idList = sessionIds.stream().map(Object::toString).toList();

        List<Object> existsResults = stringRedisTemplate.executePipelined((RedisConnection connection) -> {
            for (String sessionId : idList) {
                connection.keyCommands().exists(storePolicy.historyKey(sessionId).getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        for (int i = 0; i < idList.size(); i++) {
            String sessionId = idList.get(i);
            boolean keyExists = Boolean.TRUE.equals(existsResults.get(i));

            if (!keyExists) {
                Object interviewType = stringRedisTemplate.opsForHash().get("interview:session:type-map", sessionId);
                String type = interviewType != null ? interviewType.toString() : "UNKNOWN";

                metrics.recordFailed(type);

                Span.current().setAttribute("qfeed.session_id", sessionId);
                Span.current().setAttribute("qfeed.interview_type", type);
                Span.current().setAttribute("qfeed.session.completed", false);

                log.warn("TTL 만료로 세션 실패 처리 - sessionId={}, interviewType={}", sessionId, type);

                stringRedisTemplate.opsForHash().delete("interview:session:type-map", sessionId);
            }
            stringRedisTemplate.opsForZSet().remove(indexKey, sessionId);
        }
    }
}