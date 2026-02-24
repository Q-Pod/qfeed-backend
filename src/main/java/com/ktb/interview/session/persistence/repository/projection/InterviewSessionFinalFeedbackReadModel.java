package com.ktb.interview.session.persistence.repository.projection;

import java.math.BigDecimal;

/**
 * 세션 최종 피드백 단일 조회용 읽기 모델.
 */
public interface InterviewSessionFinalFeedbackReadModel {

    String getSessionId();

    Long getAccountId();

    String getInterviewType();

    Long getInitialQuestionId();

    String getSessionStatus();

    String getFeedbackSessionId();

    String getBadCaseType();

    String getBadCaseMessage();

    String getBadCaseGuidance();

    String getKeywordCoveredJson();

    String getKeywordMissingJson();

    BigDecimal getKeywordCoverageRatio();

    String getOverallStrengths();

    String getOverallImprovements();

    String getMetricsJson();

    String getTopicsJson();

    String getHistoryJson();
}
