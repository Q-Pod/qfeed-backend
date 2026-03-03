package com.ktb.interview.application.service.flow;

import com.ktb.answer.domain.Answer;
import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.repository.AnswerRepository;
import com.ktb.answer.service.AnswerDomainService;
import com.ktb.hashtag.domain.QuestionHashtag;
import com.ktb.hashtag.repository.QuestionHashtagRepository;
import com.ktb.interview.application.InterviewFeedbackOrchestrator;
import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.mapper.InterviewSubmissionResponseMapper;
import com.ktb.interview.port.out.InterviewSessionFinalFeedbackStore;
import com.ktb.interview.session.domain.InterviewHistoryItem;
import com.ktb.interview.session.domain.InterviewQuestionSnapshot;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.domain.InterviewSessionFeedback;
import com.ktb.interview.session.domain.InterviewSessionStatus;
import com.ktb.interview.session.dto.response.InterviewSessionFinalFeedbackResponse;
import com.ktb.interview.session.exception.InterviewSessionInvalidStateException;
import com.ktb.interview.session.mapper.InterviewSessionFeedbackMapper;
import com.ktb.interview.session.persistence.InterviewFinalFeedbackPersistenceService;
import com.ktb.interview.session.repository.InterviewSessionFeedbackRepository;
import com.ktb.interview.session.service.InterviewSessionService;
import com.ktb.interview.validator.InterviewSubmissionValidator;
import com.ktb.question.domain.Question;
import com.ktb.question.domain.QuestionType;
import com.ktb.question.exception.QuestionNotFoundException;
import com.ktb.question.repository.QuestionRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세션 최종 피드백 생성/저장 플로우를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewSessionFinalFeedbackFlowService {

    private static final String SESSION_STATUS_COMPLETED = InterviewSessionStatus.COMPLETED.name();
    private static final String ERROR_EMPTY_INTERVIEW_HISTORY_FOR_FINAL_FEEDBACK =
            "interview history is empty for final feedback";
    private static final String ERROR_PRACTICE_QUESTION_ID_MISSING =
            "practice interview questionId is missing in session history";

    private final AnswerRepository answerRepository;
    private final AnswerDomainService answerDomainService;
    private final QuestionRepository questionRepository;
    private final QuestionHashtagRepository questionHashtagRepository;
    private final InterviewSessionService interviewSessionService;
    private final InterviewSessionFinalFeedbackStore finalFeedbackStore;
    private final InterviewSubmissionValidator submissionValidator;
    private final InterviewSubmissionResponseMapper responseMapper;
    private final InterviewSessionFeedbackRepository feedbackRepository;
    private final InterviewFeedbackOrchestrator interviewFeedbackOrchestrator;
    private final InterviewFinalFeedbackPersistenceService finalFeedbackPersistenceService;

    /**
     * 세션(연습/실전) 누적 이력 기반으로 최종 AI 피드백을 생성합니다.
     */
    @Transactional
    public InterviewSessionFinalFeedbackResponse requestSessionFinalFeedback(Long accountId, String sessionId, String clientIp) {
        log.info("requestSessionFinalFeedback - accountId={}, sessionId={}, clientIp={}", accountId, sessionId, clientIp);

        InterviewSession session = interviewSessionService.getSession(accountId, sessionId);
        submissionValidator.validateSessionReadyForFinalFeedback(session);

        Optional<InterviewSessionFeedback> cached = feedbackRepository.findBySessionId(sessionId);
        if (cached.isPresent()) {
            log.info("requestSessionFinalFeedback using cached feedback - sessionId={}", sessionId);
            session.markCompleted();
            InterviewFeedbackDataResponse cachedFeedback = InterviewSessionFeedbackMapper.toDto(cached.get())
                    .withStatus(SESSION_STATUS_COMPLETED);
            finalFeedbackStore.persistFinalFeedback(session, cachedFeedback);
            interviewSessionService.deleteSession(session.getSessionId());
            return responseMapper.toSessionFinalFeedbackResponse(
                    cachedFeedback,
                    session.getInterviewType(),
                    session.getInterviewHistoryView()
            );
        }

        List<InterviewHistoryItem> history = session.getInterviewHistoryView();
        if (history.isEmpty()) {
            log.warn("requestSessionFinalFeedback failed - empty history, sessionId={}", sessionId);
            throw new InterviewSessionInvalidStateException(ERROR_EMPTY_INTERVIEW_HISTORY_FOR_FINAL_FEEDBACK);
        }
        log.debug("requestSessionFinalFeedback history resolved - sessionId={}, historySize={}", sessionId, history.size());

        InterviewHistoryItem latestTurn = history.get(history.size() - 1);
        Question persistenceQuestion = resolvePersistenceQuestionForFinalFeedback(session, latestTurn);

        String finalAnswerText = latestTurn.answerText();
        if (finalAnswerText == null || finalAnswerText.isBlank()) {
            finalAnswerText = "(final feedback request)";
        }

        Answer answer = createProcessingAnswer(
                accountId,
                persistenceQuestion,
                finalAnswerText,
                session.getSessionId(),
                session.getInterviewType()
        );
        List<QuestionHashtag> questionHashtags = questionHashtagRepository.findKeywordNamesByQuestionId(persistenceQuestion.getId());
        log.debug("requestSessionFinalFeedback anchor resolved - sessionId={}, questionId={}, hashtagCount={}",
                sessionId, persistenceQuestion.getId(), questionHashtags.size());

        InterviewFeedbackDataResponse feedback = interviewFeedbackOrchestrator.generateFeedback(
                accountId,
                session,
                persistenceQuestion,
                answer,
                history,
                questionHashtags
        );
        finalFeedbackPersistenceService.persistAnswerFeedback(answer, feedback, questionHashtags);

        InterviewFeedbackDataResponse completed = responseMapper.toFinalSessionFeedbackResponse(feedback, answer.getId());
        session.markCompleted();
        finalFeedbackStore.persistFinalFeedback(session, completed);
        interviewSessionService.deleteSession(session.getSessionId());
        log.info("requestSessionFinalFeedback completed - accountId={}, sessionId={}, answerId={}",
                accountId, sessionId, answer.getId());
        return responseMapper.toSessionFinalFeedbackResponse(
                completed,
                session.getInterviewType(),
                session.getInterviewHistoryView()
        );
    }

    /**
     * AI 처리 상태의 Answer 엔티티를 생성하고 세션 ID를 연결해 저장합니다.
     */
    private Answer createProcessingAnswer(
            Long accountId,
            Question question,
            String answerText,
            String sessionId,
            AnswerType answerType
    ) {
        Answer answer = answerDomainService.createAnswer(accountId, question.getId(), answerText, answerType);
        answer.assignSessionId(sessionId);
        answerRepository.save(answer);
        return answer;
    }

    /**
     * 세션 최종 피드백 생성 시 영속/통계용 질문 앵커를 선택합니다.
     */
    private Question resolvePersistenceQuestionForFinalFeedback(InterviewSession session, InterviewHistoryItem latestTurn) {
        if (session.getInterviewType() == AnswerType.PRACTICE_INTERVIEW) {
            if (latestTurn.questionId() == null) {
                throw new InterviewSessionInvalidStateException(ERROR_PRACTICE_QUESTION_ID_MISSING);
            }
            return questionRepository.findById(latestTurn.questionId())
                    .orElseThrow(() -> new QuestionNotFoundException(latestTurn.questionId()));
        }
        return resolvePersistenceQuestionForReal(session);
    }

    /**
     * real 모드에서 현재 질문 snapshot에 DB 질문 ID가 없을 수 있어, 영속/통계용 질문 앵커를 선택합니다.
     */
    private Question resolvePersistenceQuestionForReal(InterviewSession session) {
        Long initialQuestionId = session.getInitialQuestionId();
        if (initialQuestionId != null) {
            return questionRepository.findById(initialQuestionId)
                    .orElseThrow(() -> new QuestionNotFoundException(initialQuestionId));
        }

        InterviewQuestionSnapshot snapshot = session.getCurrentQuestion();
        QuestionType questionType = session.getQuestionType();
        if (snapshot == null) {
            return questionRepository.findRandomActiveByType(questionType.name())
                    .orElseThrow(() -> new QuestionNotFoundException(0L));
        }
        if (snapshot.questionId() != null) {
            return questionRepository.findById(snapshot.questionId())
                    .orElseThrow(() -> new QuestionNotFoundException(snapshot.questionId()));
        }
        if (snapshot.category() != null) {
            return questionRepository.findRandomActiveByTypeAndCategory(
                            questionType.name(),
                            snapshot.category().name()
                    )
                    .orElseGet(() -> questionRepository.findRandomActiveByType(questionType.name())
                            .orElseThrow(() -> new QuestionNotFoundException(0L)));
        }
        return questionRepository.findRandomActiveByType(questionType.name())
                .orElseThrow(() -> new QuestionNotFoundException(0L));
    }
}
