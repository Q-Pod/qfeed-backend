package com.ktb.interview.session.persistence;

import com.ktb.answer.domain.Answer;
import com.ktb.hashtag.domain.QuestionHashtag;
import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import java.util.List;

/**
 * 최종 AI 피드백 결과를 Answer 연관 저장소에 영속화하는 포트입니다.
 */
public interface InterviewFinalFeedbackPersistenceService {

    /**
     * Answer/Metric/Keyword 결과를 저장합니다.
     */
    void persistAnswerFeedback(
            Answer answer,
            InterviewFeedbackDataResponse feedback,
            List<QuestionHashtag> questionHashtags
    );
}
