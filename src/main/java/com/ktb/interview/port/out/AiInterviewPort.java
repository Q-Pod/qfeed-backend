package com.ktb.interview.port.out;

import com.ktb.answer.dto.ai.InterviewFeedbackApiResponse;
import com.ktb.answer.dto.ai.InterviewFeedbackRequest;
import com.ktb.answer.dto.ai.InterviewFollowUpQuestionApiResponse;
import com.ktb.answer.dto.ai.InterviewFollowUpQuestionRequest;

/**
 * 인터뷰 도메인이 AI 서버와 통신하기 위한 outbound port.
 */
public interface AiInterviewPort {

    /**
     * 누적 면접 이력을 AI 서버에 전달해 최종 피드백을 요청합니다.
     */
    InterviewFeedbackApiResponse requestFeedback(InterviewFeedbackRequest request);

    /**
     * 실전 세션 이력을 기반으로 다음 질문 또는 종료 결정을 요청합니다.
     */
    InterviewFollowUpQuestionApiResponse requestFollowUpQuestion(InterviewFollowUpQuestionRequest request);
}
