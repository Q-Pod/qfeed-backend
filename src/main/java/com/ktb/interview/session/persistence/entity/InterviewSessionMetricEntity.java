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
 * 세션 최종 피드백의 메트릭 결과 엔티티.
 */
@Entity
@Table(name = "interview_session_metric")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewSessionMetricEntity {

    @EmbeddedId
    private InterviewSessionMetricId id;

    @Column(name = "metric_score", nullable = false)
    private Integer metricScore;

    @Column(name = "metric_comment", columnDefinition = "TEXT")
    private String metricComment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static InterviewSessionMetricEntity create(
            String sessionId,
            Long metricId,
            Integer metricScore,
            String metricComment,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        InterviewSessionMetricEntity entity = new InterviewSessionMetricEntity();
        entity.id = new InterviewSessionMetricId(sessionId, metricId);
        entity.metricScore = metricScore;
        entity.metricComment = metricComment;
        entity.createdAt = createdAt;
        entity.updatedAt = updatedAt;
        return entity;
    }
}
