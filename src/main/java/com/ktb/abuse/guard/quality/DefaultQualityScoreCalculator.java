package com.ktb.abuse.guard.quality;

import com.ktb.abuse.config.AbuseGuardProperties;
import com.ktb.abuse.core.AbuseCheckContext;
import com.ktb.abuse.core.AbuseGuardResult;
import com.ktb.abuse.guard.Guard;
import com.ktb.abuse.util.ContentAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "abuse.guard.quality-score", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DefaultQualityScoreCalculator implements Guard {

    private static final String GUARD_NAME = "QualityScoreCalculator";
    private static final int ORDER = 4;

    private static final int WEIGHT_LENGTH = 30;
    private static final int WEIGHT_VALID_CHARS = 25;
    private static final int WEIGHT_LOW_REPETITION = 25;
    private static final int WEIGHT_LOW_WHITESPACE = 20;

    private static final int OPTIMAL_LENGTH = 200;
    private static final int MAX_LENGTH_BONUS = 500;

    private final AbuseGuardProperties properties;

    @Override
    public AbuseGuardResult check(AbuseCheckContext context) {
        String content = context.getContent();
        if (content == null || content.isBlank()) {
            return AbuseGuardResult.reject(GUARD_NAME, "답변 내용이 비어있습니다");
        }

        int qualityScore = calculateQualityScore(content);

        if (qualityScore < properties.getMinScoreForAccept()) {
            log.warn("Quality score too low for accept - accountId: {}, score: {}",
                    context.getAccountId(), qualityScore);
            return AbuseGuardResult.reject(GUARD_NAME,
                    String.format("답변 품질 점수가 너무 낮습니다 (점수: %d, 최소: %d)",
                            qualityScore, properties.getMinScoreForAccept()));
        }

        if (qualityScore < properties.getMinScoreForFeedback()) {
            log.info("Quality score below feedback threshold - accountId: {}, score: {}",
                    context.getAccountId(), qualityScore);
            return AbuseGuardResult.acceptNoFeedback(GUARD_NAME,
                    String.format("품질 점수가 피드백 기준 미달 (점수: %d, 기준: %d)",
                            qualityScore, properties.getMinScoreForFeedback()),
                    qualityScore);
        }

        return AbuseGuardResult.accept(qualityScore);
    }

    private int calculateQualityScore(String content) {
        int lengthScore = calculateLengthScore(content);
        int validCharsScore = calculateValidCharsScore(content);
        int repetitionScore = calculateRepetitionScore(content);
        int whitespaceScore = calculateWhitespaceScore(content);

        int totalScore = (lengthScore * WEIGHT_LENGTH
                + validCharsScore * WEIGHT_VALID_CHARS
                + repetitionScore * WEIGHT_LOW_REPETITION
                + whitespaceScore * WEIGHT_LOW_WHITESPACE) / 100;

        return Math.min(100, Math.max(0, totalScore));
    }

    private int calculateLengthScore(String content) {
        int length = ContentAnalyzer.calculateContentLength(content);
        if (length >= MAX_LENGTH_BONUS) {
            return 100;
        }
        if (length >= OPTIMAL_LENGTH) {
            return 80 + (int) ((length - OPTIMAL_LENGTH) * 20.0 / (MAX_LENGTH_BONUS - OPTIMAL_LENGTH));
        }
        return (int) (length * 80.0 / OPTIMAL_LENGTH);
    }

    private int calculateValidCharsScore(String content) {
        double ratio = ContentAnalyzer.calculateKoreanEnglishRatio(content);
        return (int) (ratio * 100);
    }

    private int calculateRepetitionScore(String content) {
        int maxRepeat = ContentAnalyzer.findMaxRepeatCharCount(content);
        if (maxRepeat <= 2) {
            return 100;
        }
        if (maxRepeat <= 3) {
            return 80;
        }
        if (maxRepeat <= 5) {
            return 50;
        }
        return Math.max(0, 100 - maxRepeat * 10);
    }

    private int calculateWhitespaceScore(String content) {
        double ratio = ContentAnalyzer.calculateWhitespaceRatio(content);
        if (ratio <= 0.1) {
            return 100;
        }
        if (ratio <= 0.2) {
            return 80;
        }
        if (ratio <= 0.3) {
            return 60;
        }
        return Math.max(0, (int) ((1 - ratio) * 100));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public String getName() {
        return GUARD_NAME;
    }
}
