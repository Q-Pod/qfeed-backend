package com.ktb.interview.session.repository.impl;

import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.repository.InterviewSessionRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 인메모리 인터뷰 세션 히스토리 저장소 구현.
 */
@Component
public class InMemoryInterviewSessionRepository implements InterviewSessionRepository {

    private final Map<String, InterviewSession> sessions = new ConcurrentHashMap<>();

    /**
     * 세션을 인메모리 맵에 저장합니다.
     */
    @Override
    public void save(InterviewSession session) {
        sessions.put(session.getSessionId(), session);
    }

    /**
     * 세션 ID로 세션을 조회합니다.
     */
    @Override
    public Optional<InterviewSession> findBySessionId(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    /**
     * 세션 ID로 저장된 세션을 제거합니다.
     */
    @Override
    public void deleteBySessionId(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * 현재 저장된 세션 컬렉션을 반환합니다.
     */
    @Override
    public Collection<InterviewSession> findAll() {
        return sessions.values();
    }

    /**
     * 만료된 세션을 제거하고 제거 건수를 반환합니다.
     */
    @Override
    public int removeExpired(LocalDateTime now) {
        int removed = 0;
        for (Map.Entry<String, InterviewSession> entry : sessions.entrySet()) {
            InterviewSession session = entry.getValue();
            if (session.isExpired(now)) {
                sessions.remove(entry.getKey());
                removed += 1;
            }
        }
        return removed;
    }
}
