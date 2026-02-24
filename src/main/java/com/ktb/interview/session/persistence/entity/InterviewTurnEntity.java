package com.ktb.interview.session.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 세션 내 질문/답변 턴 스냅샷 엔티티.
 */
@Entity
@Table(name = "interview_turn")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewTurnEntity {

    @EmbeddedId
    private InterviewTurnId id;

    @Column(name = "topic_id", nullable = false)
    private Integer topicId;

    @Column(name = "turn_type_cd", nullable = false, length = 20)
    private String turnType;

    @Column(name = "question_ctg", length = 50)
    private String questionCategory;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static InterviewTurnEntity create(
            String sessionId,
            int turnOrder,
            Integer topicId,
            String turnType,
            String questionCategory,
            String questionText,
            String answerText,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        InterviewTurnEntity entity = new InterviewTurnEntity();
        entity.id = new InterviewTurnId(sessionId, turnOrder);
        entity.topicId = topicId;
        entity.turnType = turnType;
        entity.questionCategory = questionCategory;
        entity.questionText = questionText;
        entity.answerText = answerText;
        entity.createdAt = createdAt;
        entity.updatedAt = updatedAt;
        return entity;
    }
}
