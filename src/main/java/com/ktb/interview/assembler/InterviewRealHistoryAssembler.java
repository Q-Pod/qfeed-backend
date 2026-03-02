package com.ktb.interview.assembler;

import com.ktb.answer.domain.TurnType;
import com.ktb.interview.dto.ai.InterviewHistoryRequest;
import com.ktb.interview.session.domain.InterviewHistoryItem;
import com.ktb.interview.session.domain.InterviewQuestionSnapshot;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.exception.InterviewSessionInvalidInputException;
import com.ktb.interview.session.exception.InterviewSessionInvalidStateException;
import com.ktb.question.domain.QuestionCategory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 실전 제출 시 세션 interview_history 구성 및 동기화를 담당합니다.
 */
@Component
public class InterviewRealHistoryAssembler {

    private static final String ERROR_CURRENT_QUESTION_NOT_PREPARED = "current question is not prepared for session";
    private static final String ERROR_INTERVIEW_HISTORY_REQUIRED = "interviewHistory must not be empty";
    private static final String ERROR_INTERVIEW_HISTORY_GAP_TEMPLATE =
            "interviewHistory.turn_order has a gap. expected=%d, actual=%d";
    private static final String ERROR_NO_NEW_INTERVIEW_TURN = "no new interview turn to append";

    /**
     * follow-up API로 전달할 누적 interview_history를 구성합니다.
     */
    public List<InterviewHistoryRequest> resolveForFollowUp(InterviewSession session, String answerText) {
        List<InterviewHistoryRequest> base = session.getInterviewHistoryView().stream()
                .map(this::toAiHistoryRequest)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        InterviewQuestionSnapshot currentQuestion = session.getCurrentQuestion();
        if (currentQuestion == null || currentQuestion.content() == null || currentQuestion.content().isBlank()) {
            throw new InterviewSessionInvalidStateException(ERROR_CURRENT_QUESTION_NOT_PREPARED);
        }

        int nextTurnOrder = base.size();
        Integer topicId = session.getCurrentTopicId() == null ? 1 : session.getCurrentTopicId();
        String turnType = nextTurnOrder == 0 ? TurnType.NEW_TOPIC.wireValue() : TurnType.FOLLOW_UP.wireValue();
        base.add(new InterviewHistoryRequest(
                currentQuestion.content(),
                answerText,
                turnType,
                nextTurnOrder,
                topicId,
                currentQuestion.category()
        ));
        return base;
    }

    /**
     * 세션에 저장된 누적 이력과 요청 누적 이력을 동기화합니다.
     * 요청에는 전체 누적 이력이 오므로 신규 turn만 append 합니다.
     */
    public void syncSessionHistoryFromRequest(
            InterviewSession session,
            List<InterviewHistoryRequest> requestHistory,
            Long videoFileId
    ) {
        if (requestHistory == null || requestHistory.isEmpty()) {
            throw new InterviewSessionInvalidInputException(ERROR_INTERVIEW_HISTORY_REQUIRED);
        }

        int existingCount = session.getInterviewHistoryView().size();
        int latestTurnOrder = requestHistory.getLast().turnOrder();
        Map<Integer, QuestionCategory> topicCategoryMap = buildTopicCategoryMap(session);
        int appended = 0;

        for (InterviewHistoryRequest turn : requestHistory) {
            int turnOrder = turn.turnOrder();
            if (turnOrder < existingCount) {
                continue;
            }
            if (turnOrder > existingCount) {
                throw new InterviewSessionInvalidInputException(
                        String.format(ERROR_INTERVIEW_HISTORY_GAP_TEMPLATE, existingCount, turnOrder)
                );
            }

            TurnType turnType = parseHistoryTurnType(turn.turnType());
            QuestionCategory turnCategory = resolveTurnCategory(turn, session, topicCategoryMap);
            Long turnVideoFileId = turnOrder == latestTurnOrder ? videoFileId : null;
            session.appendHistory(new InterviewHistoryItem(
                    null,
                    turn.question(),
                    turnCategory,
                    turn.answerText(),
                    turnType,
                    turnOrder,
                    turn.topicId(),
                    turnVideoFileId
            ));
            if (turn.topicId() != null && turnCategory != null) {
                topicCategoryMap.put(turn.topicId(), turnCategory);
            }
            existingCount += 1;
            appended += 1;
        }

        if (appended == 0) {
            throw new InterviewSessionInvalidInputException(ERROR_NO_NEW_INTERVIEW_TURN);
        }
    }

    private InterviewHistoryRequest toAiHistoryRequest(InterviewHistoryItem item) {
        return new InterviewHistoryRequest(
                item.question(),
                item.answerText(),
                item.turnType() == TurnType.MAIN ? TurnType.NEW_TOPIC.wireValue() : item.turnType().wireValue(),
                item.turnOrder(),
                item.topicId(),
                item.category()
        );
    }

    private Map<Integer, QuestionCategory> buildTopicCategoryMap(InterviewSession session) {
        Map<Integer, QuestionCategory> topicCategoryMap = new HashMap<>();
        for (InterviewHistoryItem item : session.getInterviewHistoryView()) {
            if (item.topicId() != null && item.category() != null) {
                topicCategoryMap.putIfAbsent(item.topicId(), item.category());
            }
        }

        if (session.getCurrentTopicId() != null
                && session.getCurrentQuestion() != null
                && session.getCurrentQuestion().category() != null) {
            topicCategoryMap.putIfAbsent(session.getCurrentTopicId(), session.getCurrentQuestion().category());
        }
        return topicCategoryMap;
    }

    private QuestionCategory resolveTurnCategory(
            InterviewHistoryRequest turn,
            InterviewSession session,
            Map<Integer, QuestionCategory> topicCategoryMap
    ) {
        if (turn.category() != null) {
            return turn.category();
        }
        if (turn.topicId() != null) {
            QuestionCategory byTopic = topicCategoryMap.get(turn.topicId());
            if (byTopic != null) {
                return byTopic;
            }
        }
        if (session.getCurrentQuestion() != null && session.getCurrentQuestion().category() != null) {
            return session.getCurrentQuestion().category();
        }
        return null;
    }

    private TurnType parseHistoryTurnType(String turnType) {
        if (turnType == null || turnType.isBlank()) {
            return TurnType.FOLLOW_UP;
        }
        try {
            return TurnType.fromWireValue(turnType);
        } catch (IllegalArgumentException e) {
            return TurnType.FOLLOW_UP;
        }
    }
}
