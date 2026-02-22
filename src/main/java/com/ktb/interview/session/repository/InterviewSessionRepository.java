package com.ktb.interview.session.repository;

import com.ktb.interview.session.domain.InterviewSession;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

/**
 * 인터뷰 세션 히스토리 저장소 포트.
 */
public interface InterviewSessionRepository {

    /**
     * 세션 스냅샷을 저장하거나 갱신합니다.
     */
    void save(InterviewSession session);

    /**
     * 세션 식별자로 세션을 조회합니다.
     */
    Optional<InterviewSession> findBySessionId(String sessionId);

    /**
     * 세션 식별자 기준으로 세션을 삭제합니다.
     */
    void deleteBySessionId(String sessionId);

    /**
     * 저장된 세션 전체를 반환합니다.
     */
    Collection<InterviewSession> findAll();

    /**
     * 기준 시각 이전으로 만료된 세션을 정리하고 삭제 건수를 반환합니다.
     */
    int removeExpired(LocalDateTime now);
}
