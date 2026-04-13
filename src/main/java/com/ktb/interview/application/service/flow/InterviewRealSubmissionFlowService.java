package com.ktb.interview.application.service.flow;

import com.ktb.ai.feedback.exception.AiFeedbackServiceTemporarilyUnavailableException;
import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.domain.TurnType;
import com.ktb.interview.application.InterviewFollowUpOrchestrator;
import com.ktb.interview.assembler.InterviewRealHistoryAssembler;
import com.ktb.interview.dto.ai.InterviewHistoryRequest;
import com.ktb.interview.mapper.InterviewSubmissionResponseMapper;
import com.ktb.interview.session.config.InterviewSessionProperties;
import com.ktb.interview.session.domain.InterviewQuestionSnapshot;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.domain.InterviewSessionStatus;
import com.ktb.interview.session.dto.request.RealAnswerSubmitRequest;
import com.ktb.interview.session.dto.response.InterviewRealSubmitResponse;
import com.ktb.interview.session.service.InterviewSessionService;
import com.ktb.interview.validator.InterviewSubmissionValidator;
import com.ktb.question.domain.QuestionCategory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 실전 모드 답변 제출 플로우를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewRealSubmissionFlowService {

    private static final String ERROR_AI_FOLLOW_UP_QUESTION_EMPTY = "AI follow-up question is empty";
    private static final String MESSAGE_REAL_MAX_TURN_REACHED_TEMPLATE =
            "최대 답변 횟수(%d회)에 도달하여 면접이 종료되었습니다. 최종 피드백을 요청해주세요.";

    private final InterviewSessionProperties interviewSessionProperties;
    private final InterviewSessionService interviewSessionService;
    private final InterviewSubmissionValidator submissionValidator;
    private final InterviewRealHistoryAssembler realHistoryAssembler;
    private final InterviewSubmissionResponseMapper responseMapper;
    private final InterviewFollowUpOrchestrator interviewFollowUpOrchestrator;

    /**
     * 실전 모드 답변을 제출하고 다음 질문 또는 세션 종료 응답을 반환합니다.
     */
    public InterviewRealSubmitResponse submitReal(Long accountId, RealAnswerSubmitRequest request, String clientIp) {
        log.info("submitReal - accountId={}, sessionId={}, clientIp={}",
                accountId, request.sessionId(), clientIp);
        String answerText = submissionValidator.resolveRealAnswerText(request);

        InterviewSession session = interviewSessionService.getSession(accountId, request.sessionId());
        submissionValidator.validateSessionType(session, AnswerType.REAL_INTERVIEW);
        submissionValidator.validateSessionAvailable(session);
        submissionValidator.validateOptionalQuestionType(request.questionType(), session.getQuestionType());
        submissionValidator.validateRealSubmissionAvailable(session);

        InterviewQuestionSnapshot currentQuestionSnapshot = session.getCurrentQuestion();
        submissionValidator.validateCurrentQuestionPrepared(currentQuestionSnapshot);

        List<InterviewHistoryRequest> historyForFollowUp = realHistoryAssembler.resolveForFollowUp(session, answerText);
        submissionValidator.validateRequestQuestionSync(
                request.question(),
                currentQuestionSnapshot.content(),
                session.getSessionId()
        );
        realHistoryAssembler.syncSessionHistoryFromRequest(session, historyForFollowUp, request.videoFileId());

        int realMaxTurn = resolveRealMaxTurn();
        if (session.getTurnCount() >= realMaxTurn) {
            Integer nextTopicId = session.getCurrentTopicId() == null ? 1 : session.getCurrentTopicId();
            InterviewQuestionSnapshot endedQuestion = resolveEndedQuestion(
                    session,
                    String.format(MESSAGE_REAL_MAX_TURN_REACHED_TEMPLATE, realMaxTurn)
            );
            session.updateNextQuestion(endedQuestion, TurnType.SESSION_END, nextTopicId);
            interviewSessionService.save(session);
            log.info("submitReal reached max turn - sessionId={}, turnCount={}, maxTurn={}",
                    session.getSessionId(), session.getTurnCount(), realMaxTurn);
            return responseMapper.toRealTurnResponse(
                    session.getSessionId(),
                    endedQuestion,
                    TurnType.SESSION_END,
                    nextTopicId,
                    true
            );
        }

        InterviewFollowUpOrchestrator.FollowUpDecision followUpDecision =
                interviewFollowUpOrchestrator.decideNext(
                        session,
                        request.questionType(),
                        historyForFollowUp
                );

        if (followUpDecision.badCase()) {
            TurnType nextTurnType = resolveNextTurnType(followUpDecision.nextTurnType());
            Integer nextTopicId = resolveNextTopicId(
                    session.getCurrentTopicId(),
                    followUpDecision.nextTopicId()
            );
            nextTurnType = normalizeNextTurnTypeByTopic(nextTurnType, session.getCurrentTopicId(), nextTopicId);
            session.updateNextQuestion(session.getCurrentQuestion(), nextTurnType, nextTopicId);

            InterviewRealSubmitResponse badCaseResponse = new InterviewRealSubmitResponse(
                    session.getSessionId(),
                    InterviewSessionStatus.IN_PROGRESS.name(),
                    responseMapper.toSessionBadCaseFeedback(followUpDecision.badCaseFeedback()),
                    null,
                    false
            );
            interviewSessionService.save(session);
            return badCaseResponse;
        }

        boolean shouldEnd = shouldEndRealInterview(session, followUpDecision);
        InterviewRealSubmitResponse response;
        if (shouldEnd) {
            Integer nextTopicId = resolveNextTopicId(
                    session.getCurrentTopicId(),
                    followUpDecision.nextTopicId()
            );
            InterviewQuestionSnapshot endedQuestion = resolveEndedQuestion(session, followUpDecision.nextQuestionText());
            session.updateNextQuestion(endedQuestion, TurnType.SESSION_END, nextTopicId);
            response = responseMapper.toRealTurnResponse(
                    session.getSessionId(),
                    endedQuestion,
                    TurnType.SESSION_END,
                    nextTopicId,
                    true
            );
        } else {
            InterviewQuestionSnapshot nextQuestion = resolveNextQuestion(
                    session,
                    followUpDecision.nextQuestionText(),
                    followUpDecision.nextCategory()
            );
            TurnType nextTurnType = resolveNextTurnType(followUpDecision.nextTurnType());
            Integer nextTopicId = resolveNextTopicId(
                    session.getCurrentTopicId(),
                    followUpDecision.nextTopicId()
            );
            nextTurnType = normalizeNextTurnTypeByTopic(nextTurnType, session.getCurrentTopicId(), nextTopicId);
            session.updateNextQuestion(nextQuestion, nextTurnType, nextTopicId);
            response = responseMapper.toRealTurnResponse(
                    session.getSessionId(),
                    nextQuestion,
                    nextTurnType,
                    nextTopicId,
                    false
            );
        }

        interviewSessionService.save(session);
        return response;
    }

    /**
     * 실전 모드 인터뷰 종료 조건을 통합 평가합니다.
     */
    private boolean shouldEndRealInterview(
            InterviewSession session,
            InterviewFollowUpOrchestrator.FollowUpDecision followUpDecision
    ) {
        int realMaxTurn = resolveRealMaxTurn();
        return TurnType.SESSION_END.wireValue().equalsIgnoreCase(followUpDecision.nextTurnType())
                || followUpDecision.shouldEnd()
                || session.getTurnCount() >= realMaxTurn;
    }

    /**
     * 실전 모드 최대 turn 설정값을 조회합니다.
     */
    private int resolveRealMaxTurn() {
        return interviewSessionProperties.getRealMaxTurn();
    }

    /**
     * follow-up 응답의 next turn 타입을 내부 enum으로 변환합니다.
     */
    private TurnType resolveNextTurnType(String followUpNextTurnType) {
        if (followUpNextTurnType == null || followUpNextTurnType.isBlank()) {
            return TurnType.FOLLOW_UP;
        }

        try {
            return TurnType.fromWireValue(followUpNextTurnType);
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported next turn type from AI, fallback to follow_up - value={}", followUpNextTurnType);
            return TurnType.FOLLOW_UP;
        }
    }

    /**
     * 다음 토픽 ID 변경 여부에 따라 next turn type을 보정합니다.
     */
    private TurnType normalizeNextTurnTypeByTopic(TurnType candidate, Integer currentTopicId, Integer nextTopicId) {
        if (candidate == TurnType.SESSION_END) {
            return candidate;
        }
        if (currentTopicId == null || nextTopicId == null) {
            return candidate;
        }
        if (!currentTopicId.equals(nextTopicId)) {
            return TurnType.NEW_TOPIC;
        }
        return TurnType.FOLLOW_UP;
    }

    /**
     * 면접 종료 응답용 질문 스냅샷(종료 안내 메시지)을 생성합니다.
     */
    private InterviewQuestionSnapshot resolveEndedQuestion(InterviewSession session, String endMessage) {
        String message = (endMessage == null || endMessage.isBlank())
                ? "수고하셨습니다. 면접이 종료되었습니다."
                : endMessage.trim();
        QuestionCategory category = session.getCurrentQuestion() == null ? null : session.getCurrentQuestion().category();
        return new InterviewQuestionSnapshot(null, message, category);
    }

    /**
     * 다음 topicId 우선순위(follow-up > 현재값)를 적용합니다.
     */
    private Integer resolveNextTopicId(Integer currentTopicId, Integer followUpNextTopicId) {
        if (followUpNextTopicId != null) {
            return followUpNextTopicId;
        }
        return currentTopicId;
    }

    /**
     * AI follow-up 응답(question_text/category)으로 다음 질문 스냅샷을 생성합니다.
     */
    private InterviewQuestionSnapshot resolveNextQuestion(
            InterviewSession session,
            String followUpQuestionText,
            String followUpCategory
    ) {
        if (followUpQuestionText != null && !followUpQuestionText.isBlank()) {
            QuestionCategory category = resolveNextQuestionCategory(
                    followUpCategory,
                    session.getCurrentQuestion()
            );
            return new InterviewQuestionSnapshot(null, followUpQuestionText, category);
        }
        throw new AiFeedbackServiceTemporarilyUnavailableException(ERROR_AI_FOLLOW_UP_QUESTION_EMPTY);
    }

    /**
     * 다음 질문 카테고리를 AI 응답 우선으로 결정하고, 없으면 현재 질문 카테고리를 유지합니다.
     */
    private QuestionCategory resolveNextQuestionCategory(
            String followUpCategory,
            InterviewQuestionSnapshot currentQuestion
    ) {
        if (followUpCategory != null && !followUpCategory.isBlank()) {
            try {
                return QuestionCategory.valueOf(followUpCategory.trim());
            } catch (IllegalArgumentException ignored) {
                // ignore
            }
        }
        return currentQuestion == null ? null : currentQuestion.category();
    }
}
