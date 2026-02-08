package com.ktb.fixture;

import com.ktb.answer.domain.Answer;
import com.ktb.metric.domain.AnswerMetric;
import com.ktb.metric.domain.Metric;

import static org.mockito.Mockito.mock;

public class MetricFixture {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 5;
    private static final int MAX_NAME_LENGTH = 100;
    private static final String DEFAULT_DESCRIPTION = "평가 지표 설명";

    public static Metric createMetric() {
        return Metric.create("평가지표", DEFAULT_DESCRIPTION);
    }

    public static Metric createMetric(String name) {
        return Metric.create(name, DEFAULT_DESCRIPTION);
    }

    public static Metric createMetric(String name, String description) {
        return Metric.create(name, description);
    }

    public static Metric createMetricWithoutDescription(String name) {
        return Metric.create(name, null);
    }

    public static Metric createMetricWithMinLength() {
        return Metric.create("A", DEFAULT_DESCRIPTION);
    }

    public static Metric createMetricWithMaxLength() {
        String maxName = "A".repeat(MAX_NAME_LENGTH);
        return Metric.create(maxName, DEFAULT_DESCRIPTION);
    }

    public static String createNameExceedingMaxLength() {
        return "A".repeat(MAX_NAME_LENGTH + 1);
    }

    public static Metric createMetricWithSpaces() {
        return Metric.create("논리성 평가", DEFAULT_DESCRIPTION);
    }

    public static Metric createMetricWithSpecialCharacters() {
        return Metric.create("논리성(Logic)", DEFAULT_DESCRIPTION);
    }

    public static Metric[] createMultipleMetrics(String... names) {
        return java.util.Arrays.stream(names)
                .map(name -> Metric.create(name, DEFAULT_DESCRIPTION))
                .toArray(Metric[]::new);
    }

    public static Metric[] createUniqueMetrics(int count) {
        Metric[] metrics = new Metric[count];
        for (int i = 0; i < count; i++) {
            metrics[i] = createMetric();
        }
        return metrics;
    }

    public static Metric createLogicMetric() {
        return Metric.create("논리성", "답변의 논리적 구조와 일관성");
    }

    public static Metric createClarityMetric() {
        return Metric.create("명확성", "답변의 명확함과 이해도");
    }

    public static Metric createConcisenessMetric() {
        return Metric.create("간결성", "답변의 간결함");
    }

    public static Metric createCompletenessMetric() {
        return Metric.create("완성도", "답변의 전체적인 완성도");
    }

    public static AnswerMetric createAnswerMetric() {
        return AnswerMetric.create(
                mock(Answer.class),
                mock(Metric.class),
                3
        );
    }

    public static AnswerMetric createAnswerMetric(int score) {
        return AnswerMetric.create(
                mock(Answer.class),
                mock(Metric.class),
                score
        );
    }

    public static AnswerMetric createAnswerMetric(Answer answer, Metric metric, int score) {
        return AnswerMetric.create(answer, metric, score);
    }

    public static AnswerMetric createAnswerMetricWithAnswer(Answer answer, int score) {
        return AnswerMetric.create(answer, mock(Metric.class), score);
    }

    public static AnswerMetric createAnswerMetricWithMetric(Metric metric, int score) {
        return AnswerMetric.create(mock(Answer.class), metric, score);
    }

    public static AnswerMetric createAnswerMetricWithMinScore() {
        return createAnswerMetric(MIN_SCORE);
    }

    public static AnswerMetric createAnswerMetricWithMaxScore() {
        return createAnswerMetric(MAX_SCORE);
    }

    public static AnswerMetric createAnswerMetricWithMinScore(Answer answer, Metric metric) {
        return AnswerMetric.create(answer, metric, MIN_SCORE);
    }

    public static AnswerMetric createAnswerMetricWithMaxScore(Answer answer, Metric metric) {
        return AnswerMetric.create(answer, metric, MAX_SCORE);
    }

    public static AnswerMetric createHighScoreAnswerMetric() {
        return createAnswerMetric(5);
    }

    public static AnswerMetric createMediumScoreAnswerMetric() {
        return createAnswerMetric(3);
    }

    public static AnswerMetric createLowScoreAnswerMetric() {
        return createAnswerMetric(1);
    }

    public static AnswerMetric[] createMultipleAnswerMetrics(Answer answer, Metric[] metrics, int[] scores) {
        if (metrics.length != scores.length) {
            throw new IllegalArgumentException("metrics와 scores의 길이가 같아야 합니다");
        }

        AnswerMetric[] answerMetrics = new AnswerMetric[metrics.length];
        for (int i = 0; i < metrics.length; i++) {
            answerMetrics[i] = AnswerMetric.create(answer, metrics[i], scores[i]);
        }
        return answerMetrics;
    }

    public static int createScoreBelowMin() {
        return MIN_SCORE - 1;
    }

    public static int createScoreAboveMax() {
        return MAX_SCORE + 1;
    }

    public static String createNullName() {
        return null;
    }

    public static String createEmptyName() {
        return "";
    }

    public static String createBlankName() {
        return "   ";
    }
}
