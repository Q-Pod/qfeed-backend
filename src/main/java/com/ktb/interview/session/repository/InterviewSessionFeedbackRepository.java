package com.ktb.interview.session.repository;

import com.ktb.answer.dto.ai.InterviewFeedbackDataResponse;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 세션 단위 최종 피드백 저장소 포트.
 */
public interface InterviewSessionFeedbackRepository {

    /**
     * 세션 최종 피드백과 만료 시각을 저장합니다.
     */
    void save(String sessionId, InterviewFeedbackDataResponse feedback, LocalDateTime expiresAt);

    /**
     * 세션 식별자로 최종 피드백을 조회합니다.
     */
    Optional<InterviewFeedbackDataResponse> findBySessionId(String sessionId);

    /**
     * 세션 식별자 기준으로 저장된 피드백을 삭제합니다.
     */
    void deleteBySessionId(String sessionId);
}
