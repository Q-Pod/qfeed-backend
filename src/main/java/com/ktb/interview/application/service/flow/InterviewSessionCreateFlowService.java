package com.ktb.interview.application.service.flow;

import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.domain.TurnType;
import com.ktb.interview.session.domain.InterviewQuestionSnapshot;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.dto.request.InterviewSessionCreateRequest;
import com.ktb.interview.session.dto.response.InterviewSessionCreateResponse;
import com.ktb.interview.session.exception.InterviewSessionInvalidInputException;
import com.ktb.interview.session.exception.InterviewSessionInvalidStateException;
import com.ktb.interview.session.service.InterviewSessionService;
import com.ktb.question.domain.Question;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import com.ktb.question.repository.QuestionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 인터뷰 세션 생성 유스케이스를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewSessionCreateFlowService {

    private static final String ERROR_UNSUPPORTED_INTERVIEW_TYPE =
            "interviewType supports only PRACTICE_INTERVIEW or REAL_INTERVIEW";
    private static final String ERROR_FIRST_QUESTION_NOT_FOUND =
            "failed to generate first question from question pool";

    private final InterviewSessionService interviewSessionService;
    private final QuestionRepository questionRepository;

    /**
     * 연습/실전 모드별 초기 상태를 세팅해 인터뷰 세션을 생성합니다.
     */
    public InterviewSessionCreateResponse createSession(Long accountId, InterviewSessionCreateRequest request) {
        log.info("createSession - accountId={}, interviewType={}, questionType={}",
                accountId, request.interviewType(), request.questionType());
        validateSessionCreateRequest(request);

        if (request.interviewType() == AnswerType.PRACTICE_INTERVIEW) {
            InterviewSession session = interviewSessionService.createPracticeSession(
                    accountId,
                    request.questionType()
            );
            log.info("Practice session created - accountId={}, sessionId={}, expiresAt={}",
                    accountId, session.getSessionId(), session.getExpiresAt());

            return new InterviewSessionCreateResponse(
                    session.getSessionId(),
                    session.getInterviewType().name(),
                    session.getQuestionType().name(),
                    null,
                    null,
                    TurnType.NEW_TOPIC.wireValue(),
                    null,
                    session.getExpiresAt().toString()
            );
        }

        InterviewSession session = interviewSessionService.createRealSession(
                accountId,
                request.questionType()
        );
        log.info("Real session skeleton created - accountId={}, sessionId={}, expiresAt={}",
                accountId, session.getSessionId(), session.getExpiresAt());

        QuestionCategory randomCategory = resolveRandomCategoryForQuestionType(session.getQuestionType());
        InterviewQuestionSnapshot firstQuestion = resolveFirstQuestionFromDb(session.getQuestionType(), randomCategory);

        TurnType firstTurnType = TurnType.MAIN;
        Integer firstTopicId = 1;
        session.updateNextQuestion(firstQuestion, firstTurnType, firstTopicId);
        interviewSessionService.save(session);
        log.info("Real session first question prepared from DB - sessionId={}, topicId={}, turnType={}, category={}",
                session.getSessionId(), firstTopicId, firstTurnType, firstQuestion.category());

        return new InterviewSessionCreateResponse(
                session.getSessionId(),
                session.getInterviewType().name(),
                session.getQuestionType().name(),
                session.getCurrentQuestion() == null ? null : session.getCurrentQuestion().content(),
                session.getCurrentQuestion() == null || session.getCurrentQuestion().category() == null
                        ? null
                        : session.getCurrentQuestion().category().name(),
                firstTurnType.wireValue(),
                firstTopicId,
                session.getExpiresAt().toString()
        );
    }

    /**
     * 세션 생성 요청의 인터뷰 유형 지원 범위를 검증합니다.
     */
    private void validateSessionCreateRequest(InterviewSessionCreateRequest request) {
        if (request.interviewType() != AnswerType.PRACTICE_INTERVIEW
                && request.interviewType() != AnswerType.REAL_INTERVIEW) {
            throw new InterviewSessionInvalidInputException(ERROR_UNSUPPORTED_INTERVIEW_TYPE);
        }
    }

    /**
     * questionType 기준으로 지원되는 카테고리를 랜덤 선택합니다.
     */
    private QuestionCategory resolveRandomCategoryForQuestionType(QuestionType questionType) {
        List<QuestionCategory> candidates = new ArrayList<>();
        for (QuestionCategory category : QuestionCategory.values()) {
            if (category.supports(questionType)) {
                candidates.add(category);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }

        int index = ThreadLocalRandom.current().nextInt(candidates.size());
        return candidates.get(index);
    }

    /**
     * 랜덤 카테고리/질문타입 기준으로 첫 질문을 DB에서 조회합니다.
     */
    private InterviewQuestionSnapshot resolveFirstQuestionFromDb(QuestionType questionType, QuestionCategory randomCategory) {
        Optional<Question> question = randomCategory == null
                ? Optional.empty()
                : questionRepository.findRandomActiveByTypeAndCategory(questionType.name(), randomCategory.name());
        Question selected = question.orElseGet(() -> questionRepository.findRandomActiveByType(questionType.name())
                .orElseThrow(() -> new InterviewSessionInvalidStateException(ERROR_FIRST_QUESTION_NOT_FOUND)));
        return new InterviewQuestionSnapshot(selected.getId(), selected.getContent(), selected.getCategory());
    }
}
