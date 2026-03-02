package com.ktb.interview.session.domain;

import com.ktb.answer.domain.TurnType;
import com.ktb.question.domain.QuestionCategory;

/**
 * 세션 내 단일 Q/A turn 스냅샷.
 */
public record InterviewHistoryItem(
        Long questionId,
        String question,
        QuestionCategory category,
        String answerText,
        TurnType turnType,
        int turnOrder,
        Integer topicId,
        Long videoFileId
) {
}
