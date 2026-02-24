package com.ktb.interview.session.persistence.entity;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 세션 단위 영속화 루트 엔티티.
 */
@Entity
@Table(name = "interview_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewSessionEntity {

    @Id
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "interview_type", nullable = false, length = 30)
    private String interviewType;

    @Column(name = "question_type_cd", nullable = false, length = 50)
    private String questionType;

    @Column(name = "session_status_cd", nullable = false, length = 30)
    private String sessionStatus;

    @Column(name = "initial_question_id", nullable = false)
    private Long initialQuestionId;

    @Column(name = "session_started_at", nullable = false)
    private LocalDateTime sessionStartedAt;

    @Column(name = "session_ended_at")
    private LocalDateTime sessionEndedAt;

    @Column(name = "session_expires_at", nullable = false)
    private LocalDateTime sessionExpiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private InterviewSessionEntity(String sessionId) {
        this.sessionId = sessionId;
    }

    public static InterviewSessionEntity create(String sessionId) {
        return new InterviewSessionEntity(sessionId);
    }

    public void sync(
            Long accountId,
            String interviewType,
            String questionType,
            String sessionStatus,
            Long initialQuestionId,
            LocalDateTime sessionStartedAt,
            LocalDateTime sessionEndedAt,
            LocalDateTime sessionExpiresAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.accountId = accountId;
        this.interviewType = interviewType;
        this.questionType = questionType;
        this.sessionStatus = sessionStatus;
        this.initialQuestionId = initialQuestionId;
        this.sessionStartedAt = sessionStartedAt;
        this.sessionEndedAt = sessionEndedAt;
        this.sessionExpiresAt = sessionExpiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
