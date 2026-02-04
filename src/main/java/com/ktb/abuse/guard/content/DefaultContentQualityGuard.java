package com.ktb.abuse.guard.content;

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
@ConditionalOnProperty(prefix = "abuse.guard.content-quality", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DefaultContentQualityGuard implements Guard {

    private static final String GUARD_NAME = "ContentQualityGuard";
    private static final int ORDER = 2;
    private static final int MIN_CONTENT_LENGTH = 10;

    private final AbuseGuardProperties properties;

    @Override
    public AbuseGuardResult check(AbuseCheckContext context) {
        String content = context.getContent();

        if (content == null || content.isBlank()) {
            log.warn("Empty content - accountId: {}", context.getAccountId());
            return AbuseGuardResult.reject(GUARD_NAME, "답변 내용이 비어있습니다");
        }

        if (!ContentAnalyzer.hasMinimumLength(content, MIN_CONTENT_LENGTH)) {
            log.warn("Content too short - accountId: {}, length: {}",
                    context.getAccountId(), content.length());
            return AbuseGuardResult.reject(GUARD_NAME,
                    String.format("답변 내용이 너무 짧습니다 (최소 %d자)", MIN_CONTENT_LENGTH));
        }

        double whitespaceRatio = ContentAnalyzer.calculateWhitespaceRatio(content);
        if (whitespaceRatio > properties.getMaxWhitespaceRatio()) {
            log.warn("Excessive whitespace - accountId: {}, ratio: {}",
                    context.getAccountId(), whitespaceRatio);
            return AbuseGuardResult.reject(GUARD_NAME, "답변에 공백이 너무 많습니다");
        }

        int maxRepeat = ContentAnalyzer.findMaxRepeatCharCount(content);
        if (maxRepeat > properties.getMaxRepeatCharCount()) {
            log.warn("Excessive character repetition - accountId: {}, maxRepeat: {}",
                    context.getAccountId(), maxRepeat);
            return AbuseGuardResult.reject(GUARD_NAME, "동일 문자가 과도하게 반복되었습니다");
        }

        double koreanEnglishRatio = ContentAnalyzer.calculateKoreanEnglishRatio(content);
        if (koreanEnglishRatio < properties.getMinKoreanEnglishRatio()) {
            log.warn("Low valid character ratio - accountId: {}, ratio: {}",
                    context.getAccountId(), koreanEnglishRatio);
            return AbuseGuardResult.reject(GUARD_NAME, "유효한 문자(한글/영문) 비율이 너무 낮습니다");
        }

        return AbuseGuardResult.accept();
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
