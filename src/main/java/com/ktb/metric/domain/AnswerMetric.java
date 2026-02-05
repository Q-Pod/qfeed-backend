package com.ktb.metric.domain;

import com.ktb.answer.domain.Answer;
import com.ktb.common.domain.BaseSoftDeleteEntity;
import com.ktb.common.domain.BaseTimeEntity;
import com.ktb.metric.exception.MetricInvalidRangeException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
        name = "ANSWER_METRIC",
        indexes = {
                @Index(name = "uk_answer_metric", columnList = "answer_id, metric_id", unique = true),
                @Index(name = "idx_answer_id", columnList = "answer_id"),
                @Index(name = "idx_metric_id", columnList = "metric_id"),
                @Index(name = "idx_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"answer", "metric"})
public class AnswerMetric extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_metric_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private Answer answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    private Metric metric;

    @Column(name = "answer_metric_score", nullable = false)
    private int score;

    private final static int MIN_SCORE = 1;
    private final static int MAX_SCORE = 5;

    @Builder
    private AnswerMetric(Answer answer, Metric metric, int score) {
        validateScore(score);
        this.answer = answer;
        this.metric = metric;
        this.score = score;
    }

    public static AnswerMetric create(Answer answer, Metric metric, int score) {
        return AnswerMetric.builder()
                .answer(answer)
                .metric(metric)
                .score(score)
                .build();
    }

    public void updateScore(int score) {
        validateScore(score);
        this.score = score;
    }

    private void validateScore(int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new MetricInvalidRangeException();
        }
    }

    public String getMetricName() {
        return this.metric.getName();
    }
}
