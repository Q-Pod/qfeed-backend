package com.ktb.abuse.guard.duplicate;

import com.ktb.abuse.config.AbuseGuardProperties;
import com.ktb.abuse.core.AbuseCheckContext;
import com.ktb.abuse.core.AbuseGuardResult;
import com.ktb.abuse.guard.Guard;
import com.ktb.abuse.store.ContentHashStore;
import com.ktb.abuse.util.SimHashCalculator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "abuse.guard.duplicate", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class InMemoryDuplicateGuard implements Guard {

    private static final String GUARD_NAME = "DuplicateGuard";
    private static final int ORDER = 3;
    private static final int RECENT_HASH_LIMIT = 10;

    private final ContentHashStore contentHashStore;
    private final AbuseGuardProperties properties;

    @Override
    public AbuseGuardResult check(AbuseCheckContext context) {
        String content = context.getContent();
        if (content == null || content.isBlank()) {
            return AbuseGuardResult.accept();
        }

        String sha256Hash = SimHashCalculator.calculateSha256(content);

        if (contentHashStore.existsHash(context.getAccountId(), context.getQuestionId(), sha256Hash)) {
            log.warn("Exact duplicate detected - accountId: {}, questionId: {}",
                    context.getAccountId(), context.getQuestionId());
            return AbuseGuardResult.reject(GUARD_NAME, "동일한 답변이 이미 제출되었습니다");
        }

        String simHash = SimHashCalculator.calculateSimHash(content);
        List<String> recentSimHashes = contentHashStore.getRecentSimHashes(
                context.getAccountId(), context.getQuestionId(), RECENT_HASH_LIMIT);

        for (String existingSimHash : recentSimHashes) {
            double similarity = SimHashCalculator.calculateSimilarity(simHash, existingSimHash);
            if (similarity >= properties.getSimilarityThreshold()) {
                log.warn("Similar content detected - accountId: {}, questionId: {}, similarity: {}",
                        context.getAccountId(), context.getQuestionId(), similarity);
                return AbuseGuardResult.reject(GUARD_NAME,
                        String.format("이전 답변과 너무 유사합니다 (유사도: %.0f%%)", similarity * 100));
            }
        }

        contentHashStore.storeHash(context.getAccountId(), context.getQuestionId(), sha256Hash, simHash);

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
