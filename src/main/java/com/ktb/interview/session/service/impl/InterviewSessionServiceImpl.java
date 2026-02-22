package com.ktb.interview.session.service.impl;

import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.domain.TurnType;
import com.ktb.interview.session.InterviewSessionIdProvider;
import com.ktb.interview.session.config.InterviewSessionProperties;
import com.ktb.interview.session.domain.InterviewQuestionSnapshot;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.exception.InterviewSessionAccessDeniedException;
import com.ktb.interview.session.exception.InterviewSessionExpiredException;
import com.ktb.interview.session.exception.InterviewSessionNotFoundException;
import com.ktb.interview.session.repository.InterviewSessionFeedbackRepository;
import com.ktb.interview.session.repository.InterviewSessionRepository;
import com.ktb.interview.session.service.InterviewSessionService;
import com.ktb.question.domain.QuestionType;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 인터뷰 세션 라이프사이클 관리 구현체.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewSessionServiceImpl implements InterviewSessionService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewSessionFeedbackRepository feedbackRepository;
    private final InterviewSessionIdProvider sessionIdProvider;
    private final InterviewSessionProperties sessionProperties;

    /**
     * 인터뷰 유형별 TTL을 반영해 세션을 생성하고 저장합니다.
     */
    @Override
    public InterviewSession createSession(
            Long accountId,
            AnswerType interviewType,
            QuestionType questionType,
            InterviewQuestionSnapshot firstQuestion,
            TurnType initialTurnType,
            Integer initialTopicId
    ) {
        Duration ttl = resolveTtl(interviewType);
        InterviewSession session = new InterviewSession(
                sessionIdProvider.nextId(),
                accountId,
                interviewType,
                questionType,
                firstQuestion,
                initialTurnType,
                initialTopicId,
                ttl
        );
        sessionRepository.save(session);
        return session;
    }

    /**
     * 세션을 조회하면서 TTL을 연장합니다.
     */
    @Override
    public InterviewSession getSession(Long accountId, String sessionId) {
        InterviewSession session = getSessionWithoutTouch(accountId, sessionId);
        session.touch(resolveTtl(session.getInterviewType()));
        sessionRepository.save(session);
        return session;
    }

    /**
     * 세션을 조회하되 TTL은 변경하지 않습니다.
     */
    @Override
    public InterviewSession getSessionWithoutTouch(Long accountId, String sessionId) {
        InterviewSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new InterviewSessionNotFoundException(sessionId));

        if (!session.getAccountId().equals(accountId)) {
            throw new InterviewSessionAccessDeniedException(sessionId, accountId);
        }

        if (session.isExpired(LocalDateTime.now())) {
            session.markExpired();
            sessionRepository.deleteBySessionId(sessionId);
            feedbackRepository.deleteBySessionId(sessionId);
            throw new InterviewSessionExpiredException(sessionId);
        }

        return session;
    }

    /**
     * 세션 상태 변경을 저장합니다.
     */
    @Override
    public void save(InterviewSession session) {
        sessionRepository.save(session);
    }

    /**
     * 세션 본문과 피드백 캐시를 함께 삭제합니다.
     */
    @Override
    public void deleteSession(String sessionId) {
        sessionRepository.deleteBySessionId(sessionId);
        feedbackRepository.deleteBySessionId(sessionId);
    }

    /**
     * 만료된 세션과 피드백을 함께 정리합니다.
     */
    @Override
    public int cleanupExpiredSessions() {
        int removed = 0;
        LocalDateTime now = LocalDateTime.now();
        for (InterviewSession session : sessionRepository.findAll()) {
            if (session.isExpired(now)) {
                sessionRepository.deleteBySessionId(session.getSessionId());
                feedbackRepository.deleteBySessionId(session.getSessionId());
                removed += 1;
            }
        }
        if (removed > 0) {
            log.info("Expired interview sessions cleaned up - removed={}", removed);
        }
        return removed;
    }

    /**
     * 스케줄러 진입점에서 만료 세션 정리를 수행합니다.
     */
    @Scheduled(fixedDelayString = "${interview.session.cleanup-interval-ms:300000}")
    public void scheduledCleanup() {
        cleanupExpiredSessions();
    }

    /**
     * 인터뷰 유형별 세션 TTL을 조회합니다.
     */
    private Duration resolveTtl(AnswerType interviewType) {
        return interviewType == AnswerType.PRACTICE_INTERVIEW
                ? sessionProperties.getPracticeTtl()
                : sessionProperties.getRealTtl();
    }
}
