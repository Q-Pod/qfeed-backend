package com.ktb.interview.session.repository.impl;

import com.ktb.interview.session.config.InterviewSessionStorePolicy;
import com.ktb.interview.session.domain.InterviewSessionFeedback;
import com.ktb.interview.session.repository.InterviewSessionFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@Primary
@Profile("redis")
@RequiredArgsConstructor
public class RedisInterviewSessionFeedbackRepository implements InterviewSessionFeedbackRepository {

    private final InterviewSessionStorePolicy storePolicy;
    private final RedisTemplate<String, InterviewSessionFeedback> interviewSessionFeedbackRedisTemplate;

    @Override
    public void save(String sessionId, InterviewSessionFeedback feedback, LocalDateTime expiresAt) {
        String key = storePolicy.feedbackKey(sessionId);

        LocalDateTime resolvedExpiresAt = expiresAt != null
                ? expiresAt
                : storePolicy.fallbackFeedbackExpiresAt(LocalDateTime.now()); // fallback

        Duration ttl = Duration.between(LocalDateTime.now(), resolvedExpiresAt);
        if (ttl.isNegative()) ttl = Duration.ofMinutes(1); // 방어 처리

        interviewSessionFeedbackRedisTemplate.opsForValue().set(key, feedback, ttl);
        log.debug("RedisInterviewSessionFeedbackRepository.save - key={}, ttl={}", key, ttl);
    }

    @Override
    public Optional<InterviewSessionFeedback> findBySessionId(String sessionId) {
        String key = storePolicy.feedbackKey(sessionId);
        InterviewSessionFeedback feedback = interviewSessionFeedbackRedisTemplate.opsForValue().get(key);
        if (feedback == null) {
            log.debug("RedisInterviewSessionFeedbackRepository.findBySessionId miss - key={}", key);
            return Optional.empty();
        }
        return Optional.of(feedback);
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        String key = storePolicy.feedbackKey(sessionId);
        interviewSessionFeedbackRedisTemplate.delete(key);
        log.debug("RedisInterviewSessionFeedbackRepository.deleteBySessionId - key={}", key);
    }
}