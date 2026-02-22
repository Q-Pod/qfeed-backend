package com.ktb.metric.domain;

import com.ktb.answer.domain.Answer;
import com.ktb.common.domain.BaseTimeEntity;
import com.ktb.metric.exception.MetricInvalidRangeException;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "ANSWER_METRIC",
        indexes = {
                @Index(name = "idx_answer_id", columnList = "answer_id"),
                @Index(name = "idx_metric_id", columnList = "metric_id"),
                @Index(name = "idx_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerMetric extends BaseTimeEntity {

    @EmbeddedId
    private AnswerMetricId id;

    @Column(name = "answer_metric_score", nullable = false)
    private int score;

    @Column(name = "answer_metric_comment", columnDefinition = "TEXT")
    private String comment;

    private final static int MIN_SCORE = 1;
    private final static int MAX_SCORE = 5;

    @Builder
    private AnswerMetric(Answer answer, Metric metric, int score, String comment) {
        validateScore(score);
        this.id = new AnswerMetricId(answer, metric);
        this.score = score;
        this.comment = comment;
    }

    public static AnswerMetric create(Answer answer, Metric metric, int score) {
        return AnswerMetric.builder()
                .answer(answer)
                .metric(metric)
                .score(score)
                .comment(null)
                .build();
    }

    public static AnswerMetric createWithComment(Answer answer, Metric metric, int score, String comment) {
        return AnswerMetric.builder()
                .answer(answer)
                .metric(metric)
                .score(score)
                .comment(comment)
                .build();
    }

    public void updateScore(int score) {
        validateScore(score);
        this.score = score;
    }

    public void updateComment(String comment) {
        this.comment = comment;
    }

    private void validateScore(int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new MetricInvalidRangeException();
        }
    }

    public String getMetricName() {
        return this.id.getMetric().getName();
    }
}
