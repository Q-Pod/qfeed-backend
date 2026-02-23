package com.ktb.interview.application.mapper;

import com.ktb.interview.dto.ai.InterviewBadCaseFeedbackResponse;
import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.dto.ai.InterviewFeedbackMetricResponse;
import com.ktb.interview.dto.ai.InterviewKeywordResultResponse;
import com.ktb.interview.dto.ai.InterviewOverallFeedbackResponse;
import com.ktb.interview.dto.ai.InterviewTopicFeedbackResponse;
import com.ktb.interview.session.domain.InterviewSessionFeedback;
import java.util.List;

/**
 * 세션 피드백 도메인 모델과 API DTO 간 변환기.
 */
public final class InterviewSessionFeedbackMapper {

    private InterviewSessionFeedbackMapper() {
    }

    /**
     * API DTO를 세션 저장용 도메인 모델로 변환합니다.
     */
    public static InterviewSessionFeedback toDomain(InterviewFeedbackDataResponse source) {
        if (source == null) {
            return null;
        }
        return new InterviewSessionFeedback(
                source.answerId(),
                source.userId(),
                source.questionId(),
                source.sessionId(),
                source.status(),
                toDomain(source.badCaseFeedback()),
                toDomainMetrics(source.metrics()),
                toDomainKeyword(source.keywordResult()),
                toDomainTopics(source.topicsFeedback()),
                toDomainOverall(source.overallFeedback())
        );
    }

    /**
     * 세션 저장 도메인 모델을 API DTO로 변환합니다.
     * 완료 피드백 조회 전용이므로 next_* 필드는 비웁니다.
     */
    public static InterviewFeedbackDataResponse toDto(InterviewSessionFeedback source) {
        if (source == null) {
            return null;
        }
        return new InterviewFeedbackDataResponse(
                source.answerId(),
                source.userId(),
                source.questionId(),
                source.sessionId(),
                source.status(),
                toDto(source.badCaseFeedback()),
                toDtoMetrics(source.metrics()),
                toDtoKeyword(source.keywordResult()),
                toDtoTopics(source.topicsFeedback()),
                toDtoOverall(source.overallFeedback()),
                null,
                null,
                null,
                true
        );
    }

    private static InterviewSessionFeedback.BadCaseFeedback toDomain(InterviewBadCaseFeedbackResponse source) {
        if (source == null) {
            return null;
        }
        return new InterviewSessionFeedback.BadCaseFeedback(source.type(), source.message(), source.guidance());
    }

    private static InterviewBadCaseFeedbackResponse toDto(InterviewSessionFeedback.BadCaseFeedback source) {
        if (source == null) {
            return null;
        }
        return new InterviewBadCaseFeedbackResponse(source.type(), source.message(), source.guidance());
    }

    private static List<InterviewSessionFeedback.MetricFeedback> toDomainMetrics(List<InterviewFeedbackMetricResponse> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(metric -> new InterviewSessionFeedback.MetricFeedback(metric.name(), metric.score(), metric.comment()))
                .toList();
    }

    private static List<InterviewFeedbackMetricResponse> toDtoMetrics(List<InterviewSessionFeedback.MetricFeedback> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(metric -> new InterviewFeedbackMetricResponse(metric.name(), metric.score(), metric.comment()))
                .toList();
    }

    private static InterviewSessionFeedback.KeywordResult toDomainKeyword(InterviewKeywordResultResponse source) {
        if (source == null) {
            return null;
        }
        return new InterviewSessionFeedback.KeywordResult(
                source.coveredKeywords(),
                source.missingKeywords(),
                source.coverageRatio()
        );
    }

    private static InterviewKeywordResultResponse toDtoKeyword(InterviewSessionFeedback.KeywordResult source) {
        if (source == null) {
            return new InterviewKeywordResultResponse(List.of(), List.of(), 0.0);
        }
        return new InterviewKeywordResultResponse(
                source.coveredKeywords(),
                source.missingKeywords(),
                source.coverageRatio()
        );
    }

    private static List<InterviewSessionFeedback.TopicFeedback> toDomainTopics(List<InterviewTopicFeedbackResponse> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(topic -> new InterviewSessionFeedback.TopicFeedback(
                        topic.topicId(),
                        topic.mainQuestion(),
                        topic.strengths(),
                        topic.improvements()
                ))
                .toList();
    }

    private static List<InterviewTopicFeedbackResponse> toDtoTopics(List<InterviewSessionFeedback.TopicFeedback> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(topic -> new InterviewTopicFeedbackResponse(
                        topic.topicId(),
                        topic.mainQuestion(),
                        topic.strengths(),
                        topic.improvements()
                ))
                .toList();
    }

    private static InterviewSessionFeedback.OverallFeedback toDomainOverall(InterviewOverallFeedbackResponse source) {
        if (source == null) {
            return null;
        }
        return new InterviewSessionFeedback.OverallFeedback(source.strengths(), source.improvements());
    }

    private static InterviewOverallFeedbackResponse toDtoOverall(InterviewSessionFeedback.OverallFeedback source) {
        if (source == null) {
            return null;
        }
        return new InterviewOverallFeedbackResponse(source.strengths(), source.improvements());
    }
}
