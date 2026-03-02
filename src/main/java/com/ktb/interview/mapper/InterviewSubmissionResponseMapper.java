package com.ktb.interview.mapper;

import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.domain.TurnType;
import com.ktb.interview.dto.ai.InterviewBadCaseFeedbackResponse;
import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.dto.ai.InterviewKeywordResultResponse;
import com.ktb.interview.dto.ai.InterviewOverallFeedbackResponse;
import com.ktb.interview.dto.ai.InterviewTopicFeedbackResponse;
import com.ktb.interview.session.domain.InterviewHistoryItem;
import com.ktb.interview.session.domain.InterviewQuestionSnapshot;
import com.ktb.interview.session.domain.InterviewSessionStatus;
import com.ktb.interview.session.dto.response.InterviewFinalFeedbackMetricResponse;
import com.ktb.interview.session.dto.response.InterviewHistoryResponse;
import com.ktb.interview.session.dto.response.InterviewNextQuestionTurnResponse;
import com.ktb.interview.session.dto.response.InterviewRealSubmitResponse;
import com.ktb.interview.session.dto.response.InterviewSessionBadCaseFeedbackResponse;
import com.ktb.interview.session.dto.response.InterviewSessionFinalFeedbackResponse;
import com.ktb.interview.session.dto.response.InterviewSessionKeywordResultResponse;
import com.ktb.interview.session.dto.response.InterviewSessionOverallFeedbackResponse;
import com.ktb.interview.session.dto.response.InterviewSessionTopicFeedbackResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 제출/최종 피드백 응답 DTO 변환 책임을 담당합니다.
 */
@Component
public class InterviewSubmissionResponseMapper {

    private static final String SESSION_STATUS_IN_PROGRESS = InterviewSessionStatus.IN_PROGRESS.name();
    private static final String SESSION_STATUS_COMPLETED = InterviewSessionStatus.COMPLETED.name();

    /**
     * 실전 turn 제출 응답 모델을 생성합니다.
     */
    public InterviewRealSubmitResponse toRealTurnResponse(
            String sessionId,
            InterviewQuestionSnapshot nextQuestion,
            TurnType nextTurnType,
            Integer nextTopicId,
            boolean isFinal
    ) {
        return new InterviewRealSubmitResponse(
                sessionId,
                SESSION_STATUS_IN_PROGRESS,
                null,
                toNextTurnQuestionResponse(nextQuestion, nextTurnType, nextTopicId),
                isFinal
        );
    }

    /**
     * 세션 최종 피드백 응답 형태로 변환합니다.
     */
    public InterviewFeedbackDataResponse toFinalSessionFeedbackResponse(
            InterviewFeedbackDataResponse feedback,
            Long answerId
    ) {
        return new InterviewFeedbackDataResponse(
                answerId,
                feedback.userId(),
                feedback.questionId(),
                feedback.sessionId(),
                SESSION_STATUS_COMPLETED,
                feedback.badCaseFeedback(),
                feedback.metrics(),
                feedback.keywordResult(),
                feedback.topicsFeedback(),
                feedback.overallFeedback(),
                null,
                null,
                null,
                true
        );
    }

    /**
     * 최종 세션 피드백 API 전용 응답 모델로 변환합니다.
     * next_question/next_turn_type/next_topic_id/is_final, metrics.comment은 노출하지 않습니다.
     */
    public InterviewSessionFinalFeedbackResponse toSessionFinalFeedbackResponse(
            InterviewFeedbackDataResponse feedback,
            AnswerType interviewType,
            List<InterviewHistoryItem> interviewHistory
    ) {
        List<InterviewFinalFeedbackMetricResponse> metrics = feedback.metrics() == null
                ? List.of()
                : feedback.metrics().stream()
                .map(metric -> new InterviewFinalFeedbackMetricResponse(metric.name(), metric.score()))
                .toList();

        Long userId = interviewType == AnswerType.REAL_INTERVIEW ? null : feedback.userId();
        Long questionId = interviewType == AnswerType.REAL_INTERVIEW ? null : feedback.questionId();

        return new InterviewSessionFinalFeedbackResponse(
                feedback.answerId(),
                userId,
                questionId,
                feedback.sessionId(),
                feedback.status(),
                toSessionBadCaseFeedback(feedback.badCaseFeedback()),
                metrics,
                toSessionKeywordResult(feedback.keywordResult()),
                toSessionTopicsFeedback(feedback.topicsFeedback()),
                toSessionOverallFeedback(feedback.overallFeedback()),
                toHistoryResponses(interviewHistory)
        );
    }

    private InterviewNextQuestionTurnResponse toNextTurnQuestionResponse(
            InterviewQuestionSnapshot question,
            TurnType turnType,
            Integer topicId
    ) {
        if (question == null) {
            return null;
        }
        return new InterviewNextQuestionTurnResponse(
                question.questionId(),
                question.content(),
                question.category() == null ? null : question.category().name(),
                turnType == null ? null : turnType.wireValue(),
                topicId
        );
    }

    private List<InterviewHistoryResponse> toHistoryResponses(List<InterviewHistoryItem> interviewHistory) {
        if (interviewHistory == null || interviewHistory.isEmpty()) {
            return List.of();
        }
        return interviewHistory.stream()
                .map(item -> new InterviewHistoryResponse(
                        item.question(),
                        item.category() == null ? null : item.category().name(),
                        item.answerText(),
                        item.turnType() == null ? null : item.turnType().wireValue(),
                        item.turnOrder(),
                        item.topicId(),
                        item.videoFileId(),
                        null
                ))
                .toList();
    }

    public InterviewSessionBadCaseFeedbackResponse toSessionBadCaseFeedback(
            InterviewBadCaseFeedbackResponse badCaseFeedback
    ) {
        if (badCaseFeedback == null) {
            return null;
        }
        return new InterviewSessionBadCaseFeedbackResponse(
                badCaseFeedback.type(),
                badCaseFeedback.message(),
                badCaseFeedback.guidance()
        );
    }

    private InterviewSessionKeywordResultResponse toSessionKeywordResult(
            InterviewKeywordResultResponse keywordResult
    ) {
        if (keywordResult == null) {
            return null;
        }
        return new InterviewSessionKeywordResultResponse(
                keywordResult.coveredKeywords(),
                keywordResult.missingKeywords(),
                keywordResult.coverageRatio()
        );
    }

    private List<InterviewSessionTopicFeedbackResponse> toSessionTopicsFeedback(
            List<InterviewTopicFeedbackResponse> topicsFeedback
    ) {
        if (topicsFeedback == null) {
            return null;
        }
        return topicsFeedback.stream()
                .map(topic -> new InterviewSessionTopicFeedbackResponse(
                        topic.topicId(),
                        topic.mainQuestion(),
                        topic.strengths(),
                        topic.improvements()
                ))
                .toList();
    }

    private InterviewSessionOverallFeedbackResponse toSessionOverallFeedback(
            InterviewOverallFeedbackResponse overallFeedback
    ) {
        if (overallFeedback == null) {
            return null;
        }
        return new InterviewSessionOverallFeedbackResponse(
                overallFeedback.strengths(),
                overallFeedback.improvements()
        );
    }
}
