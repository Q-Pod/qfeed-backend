package com.ktb.interview.application.service;

import com.ktb.interview.application.InterviewSessionManagementService;
import com.ktb.interview.application.service.flow.InterviewSessionCreateFlowService;
import com.ktb.interview.application.service.flow.InterviewSessionFeedbackQueryFlowService;
import com.ktb.interview.session.dto.request.InterviewSessionCreateRequest;
import com.ktb.interview.session.dto.response.InterviewSessionCreateResponse;
import com.ktb.interview.session.dto.response.InterviewSessionFinalFeedbackResponse;
import com.ktb.interview.session.dto.response.InterviewSessionStateResponse;
import com.ktb.interview.session.dto.response.SessionFeedbackFailedResponse;
import com.ktb.interview.session.dto.response.SessionFeedbackPendingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 인터뷰 세션 관리 유스케이스의 진입점 파사드.
 */
@Service
@RequiredArgsConstructor
public class InterviewSessionManagementServiceImpl implements InterviewSessionManagementService {

    private final InterviewSessionCreateFlowService sessionCreateFlowService;
    private final InterviewSessionFeedbackQueryFlowService sessionFeedbackQueryFlowService;

    @Override
    public InterviewSessionCreateResponse createSession(Long accountId, InterviewSessionCreateRequest request) {
        return sessionCreateFlowService.createSession(accountId, request);
    }

    @Override
    public InterviewSessionStateResponse getSessionState(Long accountId, String sessionId) {
        return sessionFeedbackQueryFlowService.getSessionState(accountId, sessionId);
    }

    @Override
    public SessionFeedbackPendingResponse getSessionFeedbackPending(Long accountId, String sessionId) {
        return sessionFeedbackQueryFlowService.getSessionFeedbackPending(accountId, sessionId);
    }

    @Override
    public SessionFeedbackFailedResponse getSessionFeedbackFailed(Long accountId, String sessionId) {
        return sessionFeedbackQueryFlowService.getSessionFeedbackFailed(accountId, sessionId);
    }

    @Override
    public InterviewSessionFinalFeedbackResponse getSessionFeedbackCompleted(Long accountId, String sessionId) {
        return sessionFeedbackQueryFlowService.getSessionFeedbackCompleted(accountId, sessionId);
    }
}
