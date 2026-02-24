package com.ktb.interview.port.out;

import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.session.domain.InterviewSession;

/**
 * 최종 AI 피드백 응답 수신 직후 세션 기반 스키마에 결과를 영속화합니다.
 */
public interface InterviewSessionFinalFeedbackStore {

    /**
     * 세션/턴/메트릭/토픽 피드백을 최종 피드백 응답 기준으로 저장합니다.
     */
    void persistFinalFeedback(InterviewSession session, InterviewFeedbackDataResponse feedback);
}
