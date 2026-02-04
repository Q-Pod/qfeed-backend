package com.ktb.abuse.core;

import com.ktb.abuse.exception.DailyQuotaExceededException;
import com.ktb.abuse.exception.DuplicateContentException;
import com.ktb.abuse.exception.LowQualityContentException;
import com.ktb.abuse.exception.RateLimitExceededException;
import com.ktb.abuse.guard.Guard;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AbuseGuard {

    private final List<Guard> guards;

    public AbuseGuard(List<Guard> guards) {
        this.guards = guards.stream()
                .sorted(Comparator.comparingInt(Guard::getOrder))
                .toList();
        log.info("AbuseGuard initialized with {} guards: {}",
                guards.size(),
                guards.stream().map(Guard::getName).toList());
    }

    public AbuseGuardResult check(AbuseCheckContext context) {
        log.debug("Starting abuse check - accountId: {}, questionId: {}, contentLength: {}",
                context.getAccountId(),
                context.getQuestionId(),
                context.getContent() != null ? context.getContent().length() : 0);

        AbuseGuardResult finalResult = AbuseGuardResult.accept();

        for (Guard guard : guards) {
            AbuseGuardResult result = guard.check(context);

            log.debug("Guard {} result: status={}, reason={}",
                    guard.getName(), result.getStatus(), result.getReason());

            if (result.isRejected()) {
                log.warn("Abuse detected by {} - accountId: {}, reason: {}",
                        guard.getName(), context.getAccountId(), result.getReason());
                throwAppropriateException(guard.getName(), result.getReason());
            }

            if (result.getStatus() == AbuseGuardResult.Status.ACCEPT_NO_FEEDBACK) {
                finalResult = result;
            } else if (finalResult.shouldProvideFeedback() && result.shouldProvideFeedback()) {
                finalResult = AbuseGuardResult.accept(
                        Math.min(finalResult.getQualityScore(), result.getQualityScore()));
            }
        }

        log.info("Abuse check completed - accountId: {}, result: {}, qualityScore: {}",
                context.getAccountId(), finalResult.getStatus(), finalResult.getQualityScore());

        return finalResult;
    }

    private void throwAppropriateException(String guardName, String reason) {
        switch (guardName) {
            case "RateLimitGuard" -> throw new RateLimitExceededException(reason);
            case "ContentQualityGuard" -> throw new LowQualityContentException(reason);
            case "DuplicateGuard" -> throw new DuplicateContentException(reason);
            case "QualityScoreCalculator" -> throw new LowQualityContentException(reason);
            case "QuotaGuard" -> throw new DailyQuotaExceededException(reason);
            default -> throw new LowQualityContentException(reason);
        }
    }

    public List<String> getActiveGuards() {
        return guards.stream()
                .map(Guard::getName)
                .toList();
    }
}
