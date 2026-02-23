package com.ktb.interview.application;

import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.dto.request.InterviewSessionCreateRequest;
import com.ktb.interview.dto.response.session.InterviewSessionCreateResponse;
import com.ktb.interview.dto.response.session.InterviewSessionStateResponse;
import com.ktb.interview.dto.response.session.SessionFeedbackFailedResponse;
import com.ktb.interview.dto.response.session.SessionFeedbackPendingResponse;

/**
 * 인터뷰 세션 생성/상태/최종 피드백 조회 유스케이스 포트.
 */
public interface InterviewSessionManagementService {

    /**
     * 인터뷰 유형에 맞는 세션을 생성합니다.
     */
    InterviewSessionCreateResponse createSession(Long accountId, InterviewSessionCreateRequest request);

    /**
     * 세션 기본 상태와 누적 이력을 조회합니다.
     */
    InterviewSessionStateResponse getSessionState(Long accountId, String sessionId);

    /**
     * 세션 피드백 생성이 진행 중일 때 대기 상태 정보를 조회합니다.
     */
    SessionFeedbackPendingResponse getSessionFeedbackPending(Long accountId, String sessionId);

    /**
     * 세션 피드백 생성이 실패했을 때 실패 상세를 조회합니다.
     */
    SessionFeedbackFailedResponse getSessionFeedbackFailed(Long accountId, String sessionId);

    /**
     * 세션 피드백이 완료된 경우 최종 피드백을 조회합니다.
     */
    InterviewFeedbackDataResponse getSessionFeedbackCompleted(Long accountId, String sessionId);
}
