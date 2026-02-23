package com.ktb.interview.session.repository.impl;

import com.ktb.interview.session.config.InterviewSessionStorePolicy;
import com.ktb.interview.session.domain.InterviewSessionFeedback;
import com.ktb.interview.session.repository.InterviewSessionFeedbackRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Redis 기반 InterviewSessionFeedbackRepository 스켈레톤.
 *
 * TODO(redis 전환 시):
 * - RedisTemplate<String, InterviewSessionFeedback> 주입
 * - key 설계: interview:feedback:{sessionId}
 * - 세션 TTL과 동일하게 expire 설정
 * - delete/save를 pipeline 처리로 최적화
 */
@Slf4j
@Service
@Primary
@Profile("redis")
public class RedisInterviewSessionFeedbackRepository implements InterviewSessionFeedbackRepository {

    private final InterviewSessionStorePolicy storePolicy;

    /**
     * Redis 저장 정책(키/TTL)을 주입받아 초기화합니다.
     */
    public RedisInterviewSessionFeedbackRepository(InterviewSessionStorePolicy storePolicy) {
        this.storePolicy = storePolicy;
    }

    /**
     * Redis 인프라 전환 전까지 동작 보장을 위한 임시 메모리 저장소.
     */
    private final Map<String, StoredFeedback> feedbackByKey = new ConcurrentHashMap<>();

    /**
     * Redis 전환 전에는 정책 기반 키를 사용해 인메모리에 피드백을 저장합니다.
     */
    @Override
    public void save(String sessionId, InterviewSessionFeedback feedback, LocalDateTime expiresAt) {
        String key = storePolicy.feedbackKey(sessionId);
        LocalDateTime effectiveExpiresAt = expiresAt == null
                ? storePolicy.fallbackFeedbackExpiresAt(LocalDateTime.now())
                : expiresAt;
        feedbackByKey.put(key, new StoredFeedback(feedback, effectiveExpiresAt));
        log.debug("RedisInterviewSessionFeedbackRepository.save fallback(in-memory) - key={}, expiresAt={}",
                key, effectiveExpiresAt);
    }

    /**
     * 세션 ID로 피드백을 조회하고 만료된 데이터는 제거합니다.
     */
    @Override
    public Optional<InterviewSessionFeedback> findBySessionId(String sessionId) {
        String key = storePolicy.feedbackKey(sessionId);
        StoredFeedback stored = feedbackByKey.get(key);
        if (stored == null) {
            return Optional.empty();
        }
        if (stored.isExpired(LocalDateTime.now())) {
            feedbackByKey.remove(key);
            return Optional.empty();
        }
        return Optional.of(stored.feedback());
    }

    /**
     * 세션 ID 기반 키를 계산해 저장된 피드백을 제거합니다.
     */
    @Override
    public void deleteBySessionId(String sessionId) {
        feedbackByKey.remove(storePolicy.feedbackKey(sessionId));
    }

    private record StoredFeedback(
            InterviewSessionFeedback feedback,
            LocalDateTime expiresAt
    ) {
        /**
         * 저장 시점에 계산한 만료 시각과 현재 시각을 비교해 만료 여부를 판단합니다.
         */
        private boolean isExpired(LocalDateTime now) {
            return expiresAt != null && now.isAfter(expiresAt);
        }
    }
}
