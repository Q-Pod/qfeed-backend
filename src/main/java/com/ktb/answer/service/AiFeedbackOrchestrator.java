package com.ktb.answer.service;

import com.ktb.answer.dto.FeedbackStatus;
import com.ktb.answer.dto.response.feedback.FeedbackResponse;

/**
 * AI 피드백 오케스트레이터 인터페이스
 * 비동기 AI 피드백 생성 파이프라인 관리 (Kafka 이벤트 기반)
 */
public interface AiFeedbackOrchestrator {

    /**
     * AI 피드백 동기 조회 (FastAPI 서버 호출)
     *
     * @param answerId 답변 ID
     * @param accountId 유저 ID
     * @return AI 피드백 응답
     * @throws com.ktb.answer.exception.AnswerNotFoundException  답변을 찾을 수 없는 경우
     * @throws com.ktb.ai.feedback.exception.AiFeedbackServiceException FastAPI 서버 호출 실패 시
     */
    FeedbackResponse getFeedbackSync(Long answerId, Long accountId);

    /**
     * AI 피드백 생성 이벤트 발행 (Kafka)
     *
     * @param answerId 피드백 생성 대상 답변 ID
     * @throws RuntimeException 이벤트 발행에 실패한 경우
     */
    void enqueue(Long answerId);

    /**
     * 피드백 처리 상태 조회
     *
     * @param answerId 답변 ID
     * @return PROCESSING, COMPLETED, FAILED, FAILED_RETRYABLE 중 하나
     */
    FeedbackStatus getStatus(Long answerId);

    /**
     * 피드백 재시도 요청
     *
     * @param answerId 답변 ID
     * @throws RuntimeException 재시도가 허용되지 않는 경우
     */
    void requestRetry(Long answerId);
}
