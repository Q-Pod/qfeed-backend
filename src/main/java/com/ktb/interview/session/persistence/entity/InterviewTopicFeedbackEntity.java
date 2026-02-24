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
 * 세션 토픽별 피드백 엔티티.
 */
@Entity
@Table(name = "interview_topic_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewTopicFeedbackEntity {

    @EmbeddedId
    private InterviewTopicFeedbackId id;

    @Column(name = "main_question_text", columnDefinition = "TEXT")
    private String mainQuestionText;

    @Column(name = "strengths_feedback", columnDefinition = "TEXT")
    private String strengthsFeedback;

    @Column(name = "improvements_feedback", columnDefinition = "TEXT")
    private String improvementsFeedback;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static InterviewTopicFeedbackEntity create(
            String sessionId,
            Integer topicId,
            String mainQuestionText,
            String strengthsFeedback,
            String improvementsFeedback,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        InterviewTopicFeedbackEntity entity = new InterviewTopicFeedbackEntity();
        entity.id = new InterviewTopicFeedbackId(sessionId, topicId);
        entity.mainQuestionText = mainQuestionText;
        entity.strengthsFeedback = strengthsFeedback;
        entity.improvementsFeedback = improvementsFeedback;
        entity.createdAt = createdAt;
        entity.updatedAt = updatedAt;
        return entity;
    }
}
