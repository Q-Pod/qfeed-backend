package com.ktb.interview.application.service;

import com.ktb.interview.application.InterviewSubmissionService;
import com.ktb.interview.application.service.flow.InterviewPracticeFeedbackQueryService;
import com.ktb.interview.application.service.flow.InterviewPracticeSubmissionFlowService;
import com.ktb.interview.application.service.flow.InterviewRealSubmissionFlowService;
import com.ktb.interview.application.service.flow.InterviewSessionFinalFeedbackFlowService;
import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.session.dto.request.PracticeAnswerSubmitRequest;
import com.ktb.interview.session.dto.request.RealAnswerSubmitRequest;
import com.ktb.interview.session.dto.response.InterviewPracticeSubmitResponse;
import com.ktb.interview.session.dto.response.InterviewRealSubmitResponse;
import com.ktb.interview.session.dto.response.InterviewSessionFinalFeedbackResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 인터뷰 제출 유스케이스의 진입점 파사드.
 */
@Service
@RequiredArgsConstructor
public class InterviewSubmissionServiceImpl implements InterviewSubmissionService {

    private final InterviewPracticeSubmissionFlowService practiceSubmissionFlowService;
    private final InterviewRealSubmissionFlowService realSubmissionFlowService;
    private final InterviewSessionFinalFeedbackFlowService finalFeedbackFlowService;
    private final InterviewPracticeFeedbackQueryService practiceFeedbackQueryService;

    @Override
    public InterviewPracticeSubmitResponse submitPractice(Long accountId, PracticeAnswerSubmitRequest request, String clientIp) {
        return practiceSubmissionFlowService.submitPractice(accountId, request, clientIp);
    }

    @Override
    public InterviewRealSubmitResponse submitReal(Long accountId, RealAnswerSubmitRequest request, String clientIp) {
        return realSubmissionFlowService.submitReal(accountId, request, clientIp);
    }

    @Override
    public InterviewFeedbackDataResponse getPracticeFeedback(Long accountId, Long answerId) {
        return practiceFeedbackQueryService.getPracticeFeedback(accountId, answerId);
    }

    @Override
    public InterviewSessionFinalFeedbackResponse requestSessionFinalFeedback(Long accountId, String sessionId, String clientIp) {
        return finalFeedbackFlowService.requestSessionFinalFeedback(accountId, sessionId, clientIp);
    }
}
