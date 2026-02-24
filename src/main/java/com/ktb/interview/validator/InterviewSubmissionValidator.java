package com.ktb.interview.validator;

import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.domain.TurnType;
import com.ktb.interview.session.domain.InterviewQuestionSnapshot;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.domain.InterviewSessionStatus;
import com.ktb.interview.session.dto.request.RealAnswerSubmitRequest;
import com.ktb.interview.session.exception.InterviewSessionInvalidInputException;
import com.ktb.interview.session.exception.InterviewSessionInvalidStateException;
import com.ktb.question.domain.QuestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 인터뷰 제출 흐름에서 공통 검증 책임을 담당합니다.
 */
@Component
@Slf4j
public class InterviewSubmissionValidator {

    private static final String ERROR_CURRENT_QUESTION_NOT_PREPARED = "current question is not prepared for session";
    private static final String ERROR_PRACTICE_ALREADY_SUBMITTED =
            "practice interview answer already submitted. request final feedback";
    private static final String ERROR_PRACTICE_FINAL_FEEDBACK_UNAVAILABLE_TEMPLATE =
            "practice interview is not available for final feedback - status=%s";
    private static final String ERROR_PRACTICE_ANSWER_NOT_SUBMITTED = "practice interview answer is not submitted yet";
    private static final String ERROR_SESSION_TYPE_MISMATCH_TEMPLATE =
            "session interview type mismatch, expected=%s, actual=%s";
    private static final String ERROR_SESSION_NOT_AVAILABLE_TEMPLATE =
            "session is not available for submit - status=%s";
    private static final String ERROR_REAL_ALREADY_ENDED = "real interview already ended. request final feedback";
    private static final String ERROR_REAL_NOT_ENDED = "real interview is not ended yet";
    private static final String ERROR_QUESTION_TYPE_MISMATCH_TEMPLATE =
            "question type mismatch. expected=%s, actual=%s";
    private static final String ERROR_ANSWER_TEXT_REQUIRED = "answerText is required";
    private static final String ERROR_ANSWER_TEXT_LENGTH =
            "answerText must be between 2 and 1500 characters";
    private static final String ERROR_OPTIONAL_QUESTION_TYPE_MISMATCH_TEMPLATE =
            "questionType mismatch. expected=%s, actual=%s";

    /**
     * 세션 인터뷰 유형과 요청 경로의 인터뷰 유형 일치 여부를 검증합니다.
     */
    public void validateSessionType(InterviewSession session, AnswerType answerType) {
        if (session.getInterviewType() != answerType) {
            throw new InterviewSessionInvalidStateException(
                    String.format(ERROR_SESSION_TYPE_MISMATCH_TEMPLATE, answerType, session.getInterviewType())
            );
        }
    }

    /**
     * 제출 가능한 세션 상태인지 검증합니다.
     */
    public void validateSessionAvailable(InterviewSession session) {
        if (session.getStatus() == InterviewSessionStatus.COMPLETED
                || session.getStatus() == InterviewSessionStatus.FAILED
                || session.getStatus() == InterviewSessionStatus.EXPIRED) {
            throw new InterviewSessionInvalidStateException(
                    String.format(ERROR_SESSION_NOT_AVAILABLE_TEMPLATE, session.getStatus())
            );
        }
    }

    /**
     * 연습 모드는 단일 turn만 허용하므로 중복 제출을 차단합니다.
     */
    public void validatePracticeSubmissionAvailable(InterviewSession session) {
        if (session.getTurnCount() > 0 || session.getNextTurnType() == TurnType.SESSION_END) {
            throw new InterviewSessionInvalidStateException(ERROR_PRACTICE_ALREADY_SUBMITTED);
        }
    }

    /**
     * 인터뷰 유형별 최종 피드백 요청 가능 상태를 검증합니다.
     */
    public void validateSessionReadyForFinalFeedback(InterviewSession session) {
        if (session.getInterviewType() == AnswerType.REAL_INTERVIEW) {
            validateRealInterviewEnded(session);
            return;
        }
        validatePracticeFinalFeedbackReady(session);
    }

    /**
     * 실전 제출 가능 여부를 추가 검증합니다.
     * 세션이 이미 종료 응답 상태(session_end)이면 최종 피드백 요청 API를 호출해야 합니다.
     */
    public void validateRealSubmissionAvailable(InterviewSession session) {
        if (session.getNextTurnType() == TurnType.SESSION_END) {
            throw new InterviewSessionInvalidStateException(ERROR_REAL_ALREADY_ENDED);
        }
    }

    /**
     * 세션 질문 유형과 실제 질문 유형 일치 여부를 검증합니다.
     */
    public void validateQuestionTypeMatch(QuestionType expected, QuestionType actual) {
        if (expected != actual) {
            throw new InterviewSessionInvalidInputException(
                    String.format(ERROR_QUESTION_TYPE_MISMATCH_TEMPLATE, expected, actual)
            );
        }
    }

    /**
     * 실전 제출 요청에서 답변 텍스트를 추출하고 길이를 검증합니다.
     */
    public String resolveRealAnswerText(RealAnswerSubmitRequest request) {
        String answerText = request.answerText();
        if (answerText == null || answerText.isBlank()) {
            throw new InterviewSessionInvalidInputException(ERROR_ANSWER_TEXT_REQUIRED);
        }

        String trimmed = answerText.trim();
        if (trimmed.length() < 2 || trimmed.length() > 1500) {
            throw new InterviewSessionInvalidInputException(ERROR_ANSWER_TEXT_LENGTH);
        }
        return trimmed;
    }

    /**
     * 클라이언트가 전달한 questionType이 있으면 세션과 일치하는지 확인합니다.
     */
    public void validateOptionalQuestionType(String requestQuestionType, QuestionType sessionQuestionType) {
        if (requestQuestionType == null || requestQuestionType.isBlank()) {
            return;
        }

        if (!sessionQuestionType.name().equalsIgnoreCase(requestQuestionType.trim())) {
            throw new InterviewSessionInvalidInputException(
                    String.format(
                            ERROR_OPTIONAL_QUESTION_TYPE_MISMATCH_TEMPLATE,
                            sessionQuestionType.name(),
                            requestQuestionType
                    )
            );
        }
    }

    /**
     * 현재 질문이 유효한지(내용 포함) 검증합니다.
     */
    public void validateCurrentQuestionPrepared(InterviewQuestionSnapshot currentQuestionSnapshot) {
        if (currentQuestionSnapshot == null
                || currentQuestionSnapshot.content() == null
                || currentQuestionSnapshot.content().isBlank()) {
            throw new InterviewSessionInvalidStateException(ERROR_CURRENT_QUESTION_NOT_PREPARED);
        }
    }

    /**
     * 클라이언트 동기화용 마지막 질문 텍스트와 세션 현재 질문이 다르면 경고 로그를 남깁니다.
     */
    public void validateRequestQuestionSync(String requestQuestionText, String currentQuestionText, String sessionId) {
        if (requestQuestionText == null || requestQuestionText.isBlank()) {
            return;
        }
        if (!requestQuestionText.trim().equals(currentQuestionText == null ? null : currentQuestionText.trim())) {
            log.warn("submitReal question text mismatch - sessionId={}, requestQuestion='{}', currentQuestion='{}'",
                    sessionId, requestQuestionText, currentQuestionText);
        }
    }

    /**
     * 연습 모드는 답변 1건이 누적된 이후 최종 피드백 요청이 가능합니다.
     */
    private void validatePracticeFinalFeedbackReady(InterviewSession session) {
        if (session.getStatus() == InterviewSessionStatus.FAILED || session.getStatus() == InterviewSessionStatus.EXPIRED) {
            throw new InterviewSessionInvalidStateException(
                    String.format(ERROR_PRACTICE_FINAL_FEEDBACK_UNAVAILABLE_TEMPLATE, session.getStatus())
            );
        }
        if (session.getStatus() == InterviewSessionStatus.COMPLETED) {
            return;
        }
        if (session.getInterviewHistoryView().isEmpty()) {
            throw new InterviewSessionInvalidStateException(ERROR_PRACTICE_ANSWER_NOT_SUBMITTED);
        }
    }

    /**
     * 최종 피드백 생성 요청 시, 실전 면접이 종료 상태인지 검증합니다.
     */
    private void validateRealInterviewEnded(InterviewSession session) {
        if (session.getNextTurnType() == TurnType.SESSION_END || session.getStatus() == InterviewSessionStatus.COMPLETED) {
            return;
        }
        throw new InterviewSessionInvalidStateException(ERROR_REAL_NOT_ENDED);
    }
}
