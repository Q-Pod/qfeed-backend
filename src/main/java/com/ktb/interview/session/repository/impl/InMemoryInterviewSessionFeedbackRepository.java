package com.ktb.interview.session.repository.impl;

import com.ktb.interview.session.domain.InterviewSessionFeedback;
import com.ktb.interview.session.repository.InterviewSessionFeedbackRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 인메모리 세션 피드백 저장소 구현.
 */
@Component
public class InMemoryInterviewSessionFeedbackRepository implements InterviewSessionFeedbackRepository {

    private final Map<String, StoredFeedback> feedbackBySessionId = new ConcurrentHashMap<>();

    /**
     * 세션 ID 기준으로 최종 피드백과 만료 시각을 저장합니다.
     */
    @Override
    public void save(String sessionId, InterviewSessionFeedback feedback, LocalDateTime expiresAt) {
        feedbackBySessionId.put(sessionId, new StoredFeedback(feedback, expiresAt));
    }

    /**
     * 세션 ID로 피드백을 조회하고 만료 시 즉시 제거합니다.
     */
    @Override
    public Optional<InterviewSessionFeedback> findBySessionId(String sessionId) {
        StoredFeedback stored = feedbackBySessionId.get(sessionId);
        if (stored == null) {
            return Optional.empty();
        }
        if (stored.isExpired(LocalDateTime.now())) {
            feedbackBySessionId.remove(sessionId);
            return Optional.empty();
        }
        return Optional.of(stored.feedback());
    }

    /**
     * 세션 ID에 연결된 피드백을 제거합니다.
     */
    @Override
    public void deleteBySessionId(String sessionId) {
        feedbackBySessionId.remove(sessionId);
    }

    private record StoredFeedback(
            InterviewSessionFeedback feedback,
            LocalDateTime expiresAt
    ) {
        /**
         * 현재 시각 기준 만료 여부를 계산합니다.
         */
        private boolean isExpired(LocalDateTime now) {
            return expiresAt != null && now.isAfter(expiresAt);
        }
    }
}
