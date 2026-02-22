package com.ktb.interview.application;

import com.ktb.answer.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.answer.dto.request.PracticeAnswerSubmitRequest;
import com.ktb.answer.dto.request.RealAnswerSubmitRequest;
import com.ktb.answer.dto.response.session.InterviewPracticeSubmitResponse;
import com.ktb.answer.dto.response.session.InterviewRealSubmitResponse;
import com.ktb.answer.dto.response.session.InterviewSessionFinalFeedbackResponse;

/**
 * 인터뷰 답변 제출 유스케이스 포트.
 */
public interface InterviewSubmissionService {

    /**
     * 연습 모드 단일 turn 답변을 세션 이력에 기록합니다.
     */
    InterviewPracticeSubmitResponse submitPractice(Long accountId, PracticeAnswerSubmitRequest request, String clientIp);

    /**
     * 실전 모드 현재 turn 답변을 제출하고 다음 질문 또는 종료 정보를 반환합니다.
     */
    InterviewRealSubmitResponse submitReal(Long accountId, RealAnswerSubmitRequest request, String clientIp);

    /**
     * 저장된 연습 답변의 피드백 정보를 조회합니다.
     */
    InterviewFeedbackDataResponse getPracticeFeedback(Long accountId, Long answerId);

    /**
     * 세션(연습/실전)의 누적 이력으로 최종 AI 피드백을 생성합니다.
     */
    InterviewSessionFinalFeedbackResponse requestSessionFinalFeedback(Long accountId, String sessionId, String clientIp);
}
