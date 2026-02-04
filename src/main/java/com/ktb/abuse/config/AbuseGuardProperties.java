package com.ktb.abuse.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "abuse.guard")
@Getter
@Setter
public class AbuseGuardProperties {

    // Rate Limit
    private int userPerMinute = 3;
    private int userPerHour = 30;
    private int ipPerMinute = 15;
    private int questionCooldownSeconds = 60;
    private int questionConsecutiveLimit = 3;

    // Content Quality
    private double maxWhitespaceRatio = 0.4;
    private int maxRepeatCharCount = 5;
    private double minKoreanEnglishRatio = 0.5;

    // Duplicate
    private double similarityThreshold = 0.9;

    // Quality Score
    private int minScoreForFeedback = 60;
    private int minScoreForAccept = 30;

    // Quota
    private int dailyQuotaLimit = 15;
    private int softQuotaLimit = 10;

    // Guard 활성화 설정
    private RateLimitConfig rateLimit = new RateLimitConfig();
    private ContentQualityConfig contentQuality = new ContentQualityConfig();
    private DuplicateConfig duplicate = new DuplicateConfig();
    private QualityScoreConfig qualityScore = new QualityScoreConfig();
    private QuotaConfig quota = new QuotaConfig();

    @Getter
    @Setter
    public static class RateLimitConfig {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class ContentQualityConfig {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class DuplicateConfig {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class QualityScoreConfig {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class QuotaConfig {
        private boolean enabled = true;
    }
}
