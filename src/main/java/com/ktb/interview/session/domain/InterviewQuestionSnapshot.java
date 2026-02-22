package com.ktb.interview.session.domain;

import com.ktb.question.domain.QuestionCategory;

/**
 * 세션 진행 중 현재 질문 정보 스냅샷.
 */
public record InterviewQuestionSnapshot(
        Long questionId,
        String content,
        QuestionCategory category
) {
}
