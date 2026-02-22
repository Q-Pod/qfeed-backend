package com.ktb.interview.session.service;

import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.domain.TurnType;
import com.ktb.interview.session.domain.InterviewQuestionSnapshot;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.question.domain.QuestionType;

/**
 * 인터뷰 세션 생성/조회/수명 관리 서비스 포트.
 */
public interface InterviewSessionService {

    /**
     * 신규 인터뷰 세션을 생성합니다.
     */
    InterviewSession createSession(
            Long accountId,
            AnswerType interviewType,
            QuestionType questionType,
            InterviewQuestionSnapshot firstQuestion,
            TurnType initialTurnType,
            Integer initialTopicId
    );

    /**
     * 세션을 조회하고 TTL을 갱신합니다.
     */
    InterviewSession getSession(Long accountId, String sessionId);

    /**
     * TTL 갱신 없이 세션을 조회합니다.
     */
    InterviewSession getSessionWithoutTouch(Long accountId, String sessionId);

    /**
     * 세션 변경사항을 저장합니다.
     */
    void save(InterviewSession session);

    /**
     * 세션과 세션 피드백을 함께 삭제합니다.
     */
    void deleteSession(String sessionId);

    /**
     * 만료된 세션을 정리하고 삭제 건수를 반환합니다.
     */
    int cleanupExpiredSessions();
}
