package com.ktb.interview.application;

import com.ktb.interview.dto.ai.InterviewBadCaseFeedbackResponse;
import com.ktb.interview.dto.ai.InterviewHistoryRequest;
import com.ktb.interview.session.domain.InterviewSession;
import java.util.List;

/**
 * 다음 질문/종료 여부를 결정하는 오케스트레이터 포트.
 */
public interface InterviewFollowUpOrchestrator {

    /**
     * 실전 세션 다음 turn(꼬리질문/새토픽/종료)을 결정합니다.
     */
    FollowUpDecision decideNext(
            InterviewSession session,
            String questionTypeOverride,
            List<InterviewHistoryRequest> historyOverride
    );

    record FollowUpDecision(
            String message,
            boolean shouldEnd,
            boolean badCase,
            InterviewBadCaseFeedbackResponse badCaseFeedback,
            String endReason,
            String nextTurnType,
            Integer nextTopicId,
            String nextQuestionText,
            String nextCategory
    ) {
        /**
         * AI follow-up 판단 실패 시 피드백 결과만으로 진행하기 위한 기본값입니다.
         */
        public static FollowUpDecision fallback() {
            return new FollowUpDecision(null, false, false, null, null, null, null, null, null);
        }
    }
}
