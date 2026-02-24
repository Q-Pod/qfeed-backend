package com.ktb.interview.session.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 세션 최종 피드백 요약 엔티티.
 */
@Entity
@Table(name = "interview_session_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewSessionFeedbackEntity {

    @Id
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "bad_case_type_cd", length = 30)
    private String badCaseType;

    @Column(name = "bad_case_message", columnDefinition = "TEXT")
    private String badCaseMessage;

    @Column(name = "bad_case_guidance", columnDefinition = "TEXT")
    private String badCaseGuidance;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keyword_covered_json", columnDefinition = "jsonb")
    private List<String> keywordCovered;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keyword_missing_json", columnDefinition = "jsonb")
    private List<String> keywordMissing;

    @Column(name = "keyword_coverage_ratio", precision = 5, scale = 1)
    private BigDecimal keywordCoverageRatio;

    @Column(name = "overall_strengths_feedback", columnDefinition = "TEXT")
    private String overallStrengths;

    @Column(name = "overall_improvements_feedback", columnDefinition = "TEXT")
    private String overallImprovements;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private InterviewSessionFeedbackEntity(String sessionId) {
        this.sessionId = sessionId;
    }

    public static InterviewSessionFeedbackEntity create(String sessionId) {
        return new InterviewSessionFeedbackEntity(sessionId);
    }

    public void sync(
            String badCaseType,
            String badCaseMessage,
            String badCaseGuidance,
            List<String> keywordCovered,
            List<String> keywordMissing,
            BigDecimal keywordCoverageRatio,
            String overallStrengths,
            String overallImprovements,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.badCaseType = badCaseType;
        this.badCaseMessage = badCaseMessage;
        this.badCaseGuidance = badCaseGuidance;
        this.keywordCovered = keywordCovered;
        this.keywordMissing = keywordMissing;
        this.keywordCoverageRatio = keywordCoverageRatio;
        this.overallStrengths = overallStrengths;
        this.overallImprovements = overallImprovements;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
