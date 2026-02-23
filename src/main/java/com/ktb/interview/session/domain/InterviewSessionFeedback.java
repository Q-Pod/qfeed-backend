package com.ktb.interview.session.domain;

import java.util.List;

/**
 * 세션 완료 후 캐시되는 최종 피드백 도메인 스냅샷.
 * 외부 AI 응답 DTO와 분리해 세션 저장소 포트의 안정성을 유지한다.
 */
public record InterviewSessionFeedback(
        Long answerId,
        Long userId,
        Long questionId,
        String sessionId,
        String status,
        BadCaseFeedback badCaseFeedback,
        List<MetricFeedback> metrics,
        KeywordResult keywordResult,
        List<TopicFeedback> topicsFeedback,
        OverallFeedback overallFeedback
) {
    public InterviewSessionFeedback {
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        topicsFeedback = topicsFeedback == null ? List.of() : List.copyOf(topicsFeedback);
        keywordResult = keywordResult == null ? new KeywordResult(List.of(), List.of(), 0.0) : keywordResult;
    }

    public record BadCaseFeedback(
            String type,
            String message,
            String guidance
    ) {
    }

    public record MetricFeedback(
            String name,
            Integer score,
            String comment
    ) {
    }

    public record KeywordResult(
            List<String> coveredKeywords,
            List<String> missingKeywords,
            Double coverageRatio
    ) {
        public KeywordResult {
            coveredKeywords = coveredKeywords == null ? List.of() : List.copyOf(coveredKeywords);
            missingKeywords = missingKeywords == null ? List.of() : List.copyOf(missingKeywords);
            coverageRatio = coverageRatio == null ? 0.0 : coverageRatio;
        }
    }

    public record TopicFeedback(
            Integer topicId,
            String mainQuestion,
            String strengths,
            String improvements
    ) {
    }

    public record OverallFeedback(
            String strengths,
            String improvements
    ) {
    }
}
