package com.ktb.interview.session.repository.impl;

import com.ktb.interview.session.config.InterviewSessionStorePolicy;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.repository.InterviewSessionRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Redis 기반 InterviewSessionRepository 스켈레톤.
 *
 * TODO(redis 전환 시):
 * - RedisTemplate<String, InterviewSession> 주입
 * - key 설계: interview:session:{sessionId}
 * - expiresAt 기반 TTL 설정 및 갱신
 * - findAll/removeExpired를 scan + pipelining 방식으로 개선
 */
@Slf4j
@Service
@Primary
@Profile("redis")
public class RedisInterviewSessionRepository implements InterviewSessionRepository {

    private final InterviewSessionStorePolicy storePolicy;

    /**
     * Redis 저장 정책(키/TTL)을 주입받아 초기화합니다.
     */
    public RedisInterviewSessionRepository(InterviewSessionStorePolicy storePolicy) {
        this.storePolicy = storePolicy;
    }

    /**
     * Redis 인프라 전환 전까지 동작 보장을 위한 임시 메모리 저장소.
     * redis 프로파일 환경에서도 기능 검증이 가능하도록 둡니다.
     */
    private final Map<String, StoredSession> sessionsByKey = new ConcurrentHashMap<>();

    /**
     * Redis 전환 전에는 정책 기반 키를 사용해 인메모리에 세션을 저장합니다.
     */
    @Override
    public void save(InterviewSession session) {
        String key = storePolicy.historyKey(session.getSessionId());
        LocalDateTime expiresAt = session.getExpiresAt() == null
                ? storePolicy.resolveExpiresAt(session.getInterviewType(), LocalDateTime.now())
                : session.getExpiresAt();
        sessionsByKey.put(key, new StoredSession(session, expiresAt));
        log.debug("RedisInterviewSessionRepository.save fallback(in-memory) - key={}, expiresAt={}",
                key, expiresAt);
    }

    /**
     * 세션 ID로 조회하고 만료된 데이터는 즉시 정리합니다.
     */
    @Override
    public Optional<InterviewSession> findBySessionId(String sessionId) {
        String key = storePolicy.historyKey(sessionId);
        StoredSession stored = sessionsByKey.get(key);
        if (stored == null) {
            log.debug("RedisInterviewSessionRepository.findBySessionId miss - key={}", key);
            return Optional.empty();
        }
        if (stored.isExpired(LocalDateTime.now())) {
            sessionsByKey.remove(key);
            log.info("RedisInterviewSessionRepository.findBySessionId expired - key={}", key);
            return Optional.empty();
        }
        log.debug("RedisInterviewSessionRepository.findBySessionId hit - key={}", key);
        return Optional.of(stored.session());
    }

    /**
     * 세션 ID 기반 키를 계산해 저장소에서 제거합니다.
     */
    @Override
    public void deleteBySessionId(String sessionId) {
        String key = storePolicy.historyKey(sessionId);
        sessionsByKey.remove(key);
        log.debug("RedisInterviewSessionRepository.deleteBySessionId - key={}", key);
    }

    /**
     * 만료되지 않은 세션만 추려 반환합니다.
     */
    @Override
    public Collection<InterviewSession> findAll() {
        LocalDateTime now = LocalDateTime.now();
        Collection<InterviewSession> result = new ArrayList<>();
        int removed = 0;
        for (Map.Entry<String, StoredSession> entry : sessionsByKey.entrySet()) {
            StoredSession stored = entry.getValue();
            if (stored.isExpired(now)) {
                sessionsByKey.remove(entry.getKey());
                removed += 1;
                continue;
            }
            result.add(stored.session());
        }
        if (removed > 0) {
            log.info("RedisInterviewSessionRepository.findAll removed expired fallback sessions - removed={}", removed);
        }
        return result;
    }

    /**
     * 기준 시각 이전으로 만료된 세션을 제거하고 개수를 반환합니다.
     */
    @Override
    public int removeExpired(LocalDateTime now) {
        int removed = 0;
        for (Map.Entry<String, StoredSession> entry : sessionsByKey.entrySet()) {
            StoredSession stored = entry.getValue();
            if (stored.isExpired(now)) {
                sessionsByKey.remove(entry.getKey());
                removed += 1;
            }
        }
        if (removed > 0) {
            log.info("RedisInterviewSessionRepository.removeExpired - removed={}", removed);
        }
        return removed;
    }

    private record StoredSession(
            InterviewSession session,
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
