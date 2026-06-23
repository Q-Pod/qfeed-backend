package com.ktb.interview.session.repository.impl;

import com.ktb.interview.session.config.InterviewSessionStorePolicy;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.repository.InterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Primary
@Profile("redis")
@RequiredArgsConstructor
public class RedisInterviewSessionRepository implements InterviewSessionRepository {

    private final InterviewSessionStorePolicy storePolicy;
    private final RedisTemplate<String, InterviewSession> redisTemplate;

    @Override
    public void save(InterviewSession session) {
        String key = storePolicy.historyKey(session.getSessionId());

        // session의 expiresAt 기준으로 TTL 계산 — touch() 반영
        Duration ttl = Duration.between(LocalDateTime.now(), session.getExpiresAt());
        if (ttl.isNegative() || ttl.isZero()) {
            log.warn("RedisInterviewSessionRepository.save - TTL이 0 이하, 저장 생략 - key={}", key);
            return; // 이미 만료된 세션은 저장하지 않음
        }

        redisTemplate.opsForValue().set(key, session, ttl);
        log.debug("RedisInterviewSessionRepository.save - key={}, ttl={}", key, ttl);
    }

    @Override
    public Optional<InterviewSession> findBySessionId(String sessionId) {
        String key = storePolicy.historyKey(sessionId);
        InterviewSession session = redisTemplate.opsForValue().get(key);
        if (session == null) {
            log.debug("RedisInterviewSessionRepository.findBySessionId miss - key={}", key);
            return Optional.empty();
        }
        log.debug("RedisInterviewSessionRepository.findBySessionId hit - key={}", key);
        return Optional.of(session);
    }

    @Override
    public void deleteBySessionId(String sessionId) {
        String key = storePolicy.historyKey(sessionId);
        redisTemplate.delete(key);
        log.debug("RedisInterviewSessionRepository.deleteBySessionId - key={}", key);
    }

    @Override
    public Collection<InterviewSession> findAll() {
        // Redis TTL이 만료를 자동 처리 — 전체 순회 불필요
        return List.of();
    }

    @Override
    public int removeExpired(LocalDateTime now) {
        // Redis TTL이 자동 만료 처리하므로 별도 작업 불필요
        return 0;
    }
}