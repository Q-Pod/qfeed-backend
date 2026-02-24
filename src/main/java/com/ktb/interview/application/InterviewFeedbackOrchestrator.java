package com.ktb.interview.application;

import com.ktb.answer.domain.Answer;
import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.session.domain.InterviewHistoryItem;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.hashtag.domain.QuestionHashtag;
import com.ktb.question.domain.Question;
import java.util.List;

/**
 * 세션 이력을 AI 피드백 요청으로 조합/호출하는 오케스트레이터 포트.
 */
public interface InterviewFeedbackOrchestrator {

    /**
     * 세션 이력과 질문 키워드를 조합해 AI 서버 피드백을 생성합니다.
     */
    InterviewFeedbackDataResponse generateFeedback(
            Long accountId,
            InterviewSession session,
            Question question,
            Answer answer,
            List<InterviewHistoryItem> history,
            List<QuestionHashtag> questionHashtags
    );
}
