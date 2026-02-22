package com.ktb.interview.session.config;

import com.ktb.answer.domain.AnswerType;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 인터뷰 세션/피드백 스토어의 키 규칙과 TTL 정책.
 */
@Component
@Getter
public class InterviewSessionStorePolicy {

    private final InterviewSessionProperties sessionProperties;
    private final String historyKeyPrefix;
    private final String feedbackKeyPrefix;

    /**
     * 스토어 키 접두사와 TTL 정책 의존성을 초기화합니다.
     */
    public InterviewSessionStorePolicy(
            InterviewSessionProperties sessionProperties,
            @Value("${interview.session.store.history-key-prefix}") String historyKeyPrefix,
            @Value("${interview.session.store.feedback-key-prefix}") String feedbackKeyPrefix
    ) {
        this.sessionProperties = sessionProperties;
        this.historyKeyPrefix = historyKeyPrefix;
        this.feedbackKeyPrefix = feedbackKeyPrefix;
    }

    /**
     * 세션 이력 저장 키를 생성합니다.
     */
    public String historyKey(String sessionId) {
        return historyKeyPrefix + ":" + sessionId;
    }

    /**
     * 세션 피드백 저장 키를 생성합니다.
     */
    public String feedbackKey(String sessionId) {
        return feedbackKeyPrefix + ":" + sessionId;
    }

    /**
     * 인터뷰 유형별 TTL을 반환합니다.
     */
    public Duration resolveTtl(AnswerType interviewType) {
        return interviewType == AnswerType.PRACTICE_INTERVIEW
                ? sessionProperties.getPracticeTtl()
                : sessionProperties.getRealTtl();
    }

    /**
     * 현재 시각 기준 인터뷰 유형별 만료 시각을 계산합니다.
     */
    public LocalDateTime resolveExpiresAt(AnswerType interviewType, LocalDateTime now) {
        return now.plus(resolveTtl(interviewType));
    }

    /**
     * 피드백 만료 시각이 누락된 경우 사용할 기본 만료 시각을 계산합니다.
     */
    public LocalDateTime fallbackFeedbackExpiresAt(LocalDateTime now) {
        return now.plus(sessionProperties.getRealTtl());
    }
}
