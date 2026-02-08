package com.ktb.answer.dto;

import com.ktb.answer.dto.response.submit.ImmediateFeedback;
import com.ktb.answer.dto.response.common.KeywordCheck;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record ImmediateFeedbackResult(List<KeywordCheckResult> keywords) {

    public static ImmediateFeedbackResult from(ImmediateFeedback feedback) {
        if (feedback == null || feedback.keywords() == null) {
            return new ImmediateFeedbackResult(Collections.emptyList());
        }

        List<KeywordCheckResult> results = feedback.keywords().stream()
                .map(keyword -> new KeywordCheckResult(null, keyword.keyword(), keyword.included()))
                .collect(Collectors.toList());

        return new ImmediateFeedbackResult(results);
    }

    public ImmediateFeedback of() {
        if (keywords == null || keywords.isEmpty()) {
            return new ImmediateFeedback(Collections.emptyList());
        }

        List<KeywordCheck> results = keywords.stream()
            .map(keyword -> new KeywordCheck(keyword.keyword(), keyword.included()))
            .collect(Collectors.toList());

        return new ImmediateFeedback(results);
    }

    public ImmediateFeedbackResult {
        keywords = keywords == null ? Collections.emptyList() : List.copyOf(keywords);
    }
}
