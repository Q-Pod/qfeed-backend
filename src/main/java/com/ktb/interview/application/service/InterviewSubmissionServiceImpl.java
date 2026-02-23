package com.ktb.interview.application.service;

import com.ktb.answer.domain.Answer;
import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.domain.TurnType;
import com.ktb.interview.application.mapper.InterviewSessionFeedbackMapper;
import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.dto.ai.InterviewFeedbackMetricResponse;
import com.ktb.interview.dto.ai.InterviewHistoryRequest;
import com.ktb.interview.dto.ai.InterviewKeywordResultResponse;
import com.ktb.interview.dto.ai.InterviewOverallFeedbackResponse;
import com.ktb.interview.dto.request.PracticeAnswerSubmitRequest;
import com.ktb.interview.dto.request.RealAnswerSubmitRequest;
import com.ktb.interview.dto.response.session.InterviewNextQuestionTurnResponse;
import com.ktb.interview.dto.response.session.InterviewFinalFeedbackMetricResponse;
import com.ktb.interview.dto.response.session.InterviewPracticeSubmitResponse;
import com.ktb.interview.dto.response.session.InterviewRealSubmitResponse;
import com.ktb.interview.dto.response.session.InterviewSessionFinalFeedbackResponse;
import com.ktb.answer.exception.AnswerNotFoundException;
import com.ktb.answer.repository.AnswerRepository;
import com.ktb.answer.service.AnswerDomainService;
import com.ktb.interview.application.InterviewSubmissionService;
import com.ktb.interview.application.SessionFeedbackOrchestrator;
import com.ktb.interview.application.SessionFollowUpOrchestrator;
import com.ktb.interview.session.domain.InterviewHistoryItem;
import com.ktb.interview.session.domain.InterviewQuestionSnapshot;
import com.ktb.interview.session.domain.InterviewSessionFeedback;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.domain.InterviewSessionStatus;
import com.ktb.interview.session.exception.InterviewSessionInvalidInputException;
import com.ktb.interview.session.exception.InterviewSessionInvalidStateException;
import com.ktb.interview.session.repository.InterviewSessionFeedbackRepository;
import com.ktb.interview.session.service.InterviewSessionService;
import com.ktb.hashtag.domain.AnswerHashtag;
import com.ktb.hashtag.domain.Hashtag;
import com.ktb.hashtag.domain.QuestionHashtag;
import com.ktb.hashtag.repository.AnswerHashtagRepository;
import com.ktb.hashtag.repository.QuestionHashtagRepository;
import com.ktb.metric.domain.AnswerMetric;
import com.ktb.metric.domain.Metric;
import com.ktb.metric.repository.AnswerMetricRepository;
import com.ktb.metric.repository.MetricRepository;
import com.ktb.question.domain.Question;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import com.ktb.question.exception.QuestionNotFoundException;
import com.ktb.question.repository.QuestionRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 연습/실전 인터뷰 답변 제출 흐름 구현체.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewSubmissionServiceImpl implements InterviewSubmissionService {

    private static final int REAL_MAX_TURN = 15;
    private static final String SESSION_STATUS_IN_PROGRESS = InterviewSessionStatus.IN_PROGRESS.name();
    private static final String SESSION_STATUS_COMPLETED = InterviewSessionStatus.COMPLETED.name();
    private static final String ERROR_CURRENT_QUESTION_NOT_PREPARED = "current question is not prepared for session";
    private static final String ERROR_EMPTY_INTERVIEW_HISTORY_FOR_FINAL_FEEDBACK =
            "interview history is empty for final feedback";
    private static final String ERROR_INTERVIEW_HISTORY_REQUIRED = "interviewHistory must not be empty";
    private static final String ERROR_INTERVIEW_HISTORY_GAP_TEMPLATE =
            "interviewHistory.turn_order has a gap. expected=%d, actual=%d";
    private static final String ERROR_NO_NEW_INTERVIEW_TURN = "no new interview turn to append";
    private static final String ERROR_REAL_MAX_TURN_REACHED = "max turn reached for real interview session";
    private static final String ERROR_AI_FOLLOW_UP_QUESTION_EMPTY = "AI follow-up question is empty";
    private static final String ERROR_PRACTICE_ALREADY_SUBMITTED =
            "practice interview answer already submitted. request final feedback";
    private static final String ERROR_PRACTICE_FINAL_FEEDBACK_UNAVAILABLE_TEMPLATE =
            "practice interview is not available for final feedback - status=%s";
    private static final String ERROR_PRACTICE_ANSWER_NOT_SUBMITTED = "practice interview answer is not submitted yet";
    private static final String ERROR_PRACTICE_QUESTION_ID_MISSING =
            "practice interview questionId is missing in session history";
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

    private final AnswerRepository answerRepository;
    private final AnswerDomainService answerDomainService;
    private final QuestionRepository questionRepository;
    private final QuestionHashtagRepository questionHashtagRepository;
    private final AnswerHashtagRepository answerHashtagRepository;
    private final MetricRepository metricRepository;
    private final AnswerMetricRepository answerMetricRepository;
    private final InterviewSessionService interviewSessionService;
    private final InterviewSessionFeedbackRepository feedbackRepository;
    private final SessionFeedbackOrchestrator sessionFeedbackOrchestrator;
    private final SessionFollowUpOrchestrator sessionFollowUpOrchestrator;

    /**
     * 연습 모드 답변 1건을 세션 이력에 반영합니다.
     */
    @Override
    @Transactional
    public InterviewPracticeSubmitResponse submitPractice(Long accountId, PracticeAnswerSubmitRequest request, String clientIp) {
        log.info("submitPractice - accountId={}, sessionId={}, questionId={}, clientIp={}",
                accountId, request.sessionId(), request.questionId(), clientIp);
        InterviewSession session = interviewSessionService.getSession(accountId, request.sessionId());
        validateSessionType(session, AnswerType.PRACTICE_INTERVIEW);
        validateSessionAvailable(session);
        validatePracticeSubmissionAvailable(session);

        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new QuestionNotFoundException(request.questionId()));
        validateQuestionTypeMatch(session.getQuestionType(), question.getType());

        int turnOrder = session.getTurnCount();
        Integer topicId = session.getCurrentTopicId() == null ? 1 : session.getCurrentTopicId();
        InterviewHistoryItem currentTurn = new InterviewHistoryItem(
                question.getId(),
                question.getContent(),
                question.getCategory(),
                request.answerText(),
                TurnType.NEW_TOPIC,
                turnOrder,
                topicId
        );

        session.appendHistory(currentTurn);
        session.updateNextQuestion(
                new InterviewQuestionSnapshot(question.getId(), question.getContent(), question.getCategory()),
                TurnType.SESSION_END,
                topicId
        );
        interviewSessionService.save(session);

        return new InterviewPracticeSubmitResponse(
                accountId,
                question.getId(),
                session.getSessionId(),
                SESSION_STATUS_IN_PROGRESS,
                TurnType.NEW_TOPIC.wireValue(),
                turnOrder,
                topicId,
                false
        );
    }

    /**
     * 실전 모드 답변을 제출하고 다음 질문 또는 세션 종료 응답을 반환합니다.
     */
    @Override
    @Transactional
    public InterviewRealSubmitResponse submitReal(Long accountId, RealAnswerSubmitRequest request, String clientIp) {
        log.info("submitReal - accountId={}, sessionId={}, clientIp={}",
                accountId, request.sessionId(), clientIp);
        String answerText = resolveRealAnswerText(request);

        InterviewSession session = interviewSessionService.getSession(accountId, request.sessionId());
        validateSessionType(session, AnswerType.REAL_INTERVIEW);
        validateSessionAvailable(session);
        validateOptionalQuestionType(request.questionType(), session.getQuestionType());
        validateRealSubmissionAvailable(session);

        InterviewQuestionSnapshot currentQuestionSnapshot = session.getCurrentQuestion();
        if (currentQuestionSnapshot == null
                || currentQuestionSnapshot.content() == null
                || currentQuestionSnapshot.content().isBlank()) {
            throw new InterviewSessionInvalidStateException(ERROR_CURRENT_QUESTION_NOT_PREPARED);
        }

        List<InterviewHistoryRequest> historyForFollowUp = resolveRealHistoryForFollowUp(session, answerText);
        validateRequestQuestionSync(request.question(), currentQuestionSnapshot.content(), session.getSessionId());
        validateRealTurnLimit(session, historyForFollowUp);
        syncSessionHistoryFromRequest(session, historyForFollowUp);

        SessionFollowUpOrchestrator.FollowUpDecision followUpDecision =
                sessionFollowUpOrchestrator.decideNext(
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
                    SESSION_STATUS_IN_PROGRESS,
                    followUpDecision.badCaseFeedback(),
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
            response = toRealTurnResponse(
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
            response = toRealTurnResponse(
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
     * 세션(연습/실전) 누적 이력 기반으로 최종 AI 피드백을 생성합니다.
     */
    @Override
    @Transactional
    public InterviewSessionFinalFeedbackResponse requestSessionFinalFeedback(Long accountId, String sessionId, String clientIp) {
        log.info("requestSessionFinalFeedback - accountId={}, sessionId={}, clientIp={}", accountId, sessionId, clientIp);

        InterviewSession session = interviewSessionService.getSession(accountId, sessionId);
        validateSessionReadyForFinalFeedback(session);

        Optional<InterviewSessionFeedback> cached = feedbackRepository.findBySessionId(sessionId);
        if (cached.isPresent()) {
            session.markCompleted();
            interviewSessionService.save(session);
            InterviewFeedbackDataResponse cachedFeedback = InterviewSessionFeedbackMapper.toDto(cached.get())
                    .withStatus(SESSION_STATUS_COMPLETED);
            return toSessionFinalFeedbackResponse(cachedFeedback, session.getInterviewType());
        }

        List<InterviewHistoryItem> history = session.getInterviewHistoryView();
        if (history.isEmpty()) {
            throw new InterviewSessionInvalidStateException(ERROR_EMPTY_INTERVIEW_HISTORY_FOR_FINAL_FEEDBACK);
        }

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

        InterviewFeedbackDataResponse feedback = sessionFeedbackOrchestrator.generateFeedback(
                accountId,
                session,
                persistenceQuestion,
                answer,
                history,
                questionHashtags
        );
        persistFeedback(answer, feedback, questionHashtags);

        InterviewFeedbackDataResponse completed = toFinalSessionFeedbackResponse(feedback, answer.getId());
        session.markCompleted();
        feedbackRepository.save(
                session.getSessionId(),
                InterviewSessionFeedbackMapper.toDomain(completed),
                session.getExpiresAt()
        );
        interviewSessionService.save(session);
        return toSessionFinalFeedbackResponse(completed, session.getInterviewType());
    }

    /**
     * 연습 모드 답변 ID로 저장된 피드백/메트릭/키워드 결과를 조회합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public InterviewFeedbackDataResponse getPracticeFeedback(Long accountId, Long answerId) {
        Answer answer = answerRepository.findByIdWithQuestion(answerId);
        if (answer == null) {
            throw new AnswerNotFoundException(answerId);
        }
        answerDomainService.validateOwnership(answer, accountId);

        List<AnswerMetric> metrics = answerMetricRepository.findByAnswerIdWithMetric(answerId);
        List<InterviewFeedbackMetricResponse> metricResponses = metrics.stream()
                .map(metric -> new InterviewFeedbackMetricResponse(
                        metric.getMetricName(),
                        metric.getScore(),
                        metric.getComment()
                ))
                .toList();

        List<AnswerHashtag> answerHashtags = answerHashtagRepository.findByAnswerIdWithHashtag(answerId);
        InterviewKeywordResultResponse keywordResult = toKeywordResult(answerHashtags);

        return new InterviewFeedbackDataResponse(
                answer.getId(),
                accountId,
                answer.getQuestion().getId(),
                answer.getSessionId(),
                SESSION_STATUS_COMPLETED,
                null,
                metricResponses,
                keywordResult,
                null,
                new InterviewOverallFeedbackResponse(
                        answer.getStrengthsFeedback(),
                        answer.getImprovementsFeedback()
                ),
                null,
                null,
                null,
                true
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
     * AI 피드백 응답을 Answer/Metric/Hashtag 저장 구조로 분해해 영속화합니다.
     */
    private void persistFeedback(
            Answer answer,
            InterviewFeedbackDataResponse feedback,
            List<QuestionHashtag> questionHashtags
    ) {
        List<InterviewFeedbackMetricResponse> metrics = feedback.metrics() == null ? List.of() : feedback.metrics();
        saveMetrics(answer, metrics);

        InterviewOverallFeedbackResponse overall = feedback.overallFeedback();
        if (overall != null) {
            answer.setOverallFeedback(overall.strengths(), overall.improvements());
        } else {
            answer.setAiFeedback(null);
        }
        answerRepository.save(answer);

        saveKeywordResult(answer, questionHashtags, feedback.keywordResult());
    }

    /**
     * AI 메트릭을 Metric, AnswerMetric 엔티티로 매핑해 저장합니다.
     */
    private void saveMetrics(Answer answer, List<InterviewFeedbackMetricResponse> metrics) {
        if (metrics.isEmpty()) {
            return;
        }

        List<String> metricNames = metrics.stream()
                .map(InterviewFeedbackMetricResponse::name)
                .toList();

        Map<String, Metric> metricMap = new HashMap<>();
        metricRepository.findAllByNameIn(metricNames)
                .forEach(metric -> metricMap.put(metric.getName(), metric));

        List<AnswerMetric> answerMetrics = new ArrayList<>();
        for (InterviewFeedbackMetricResponse metric : metrics) {
            Metric persisted = metricMap.computeIfAbsent(metric.name(),
                    key -> metricRepository.save(Metric.create(key, "")));
            int score = metric.score() == null ? 1 : metric.score();
            answerMetrics.add(AnswerMetric.createWithComment(answer, persisted, score, metric.comment()));
        }

        answerMetricRepository.saveAll(answerMetrics);
    }

    /**
     * 질문 키워드 목록과 AI 커버 키워드 결과를 비교해 AnswerHashtag를 저장합니다.
     */
    private void saveKeywordResult(
            Answer answer,
            List<QuestionHashtag> questionHashtags,
            InterviewKeywordResultResponse keywordResult
    ) {
        Set<String> covered = new HashSet<>();
        if (keywordResult != null && keywordResult.coveredKeywords() != null) {
            keywordResult.coveredKeywords().forEach(keyword -> covered.add(keyword.toLowerCase(Locale.ROOT)));
        }

        List<AnswerHashtag> answerHashtags = questionHashtags.stream()
                .map(questionHashtag -> {
                    Hashtag hashtag = questionHashtag.getHashtag();
                    boolean included = covered.contains(hashtag.getName().toLowerCase(Locale.ROOT));
                    return AnswerHashtag.create(answer, hashtag, included);
                })
                .toList();

        answerHashtagRepository.saveAll(answerHashtags);
    }

    /**
     * 저장된 AnswerHashtag 목록을 API 응답용 키워드 결과로 변환합니다.
     */
    private InterviewKeywordResultResponse toKeywordResult(List<AnswerHashtag> answerHashtags) {
        List<String> covered = answerHashtags.stream()
                .filter(AnswerHashtag::isIncluded)
                .map(answerHashtag -> answerHashtag.getHashtag().getName())
                .toList();
        List<String> missing = answerHashtags.stream()
                .filter(answerHashtag -> !answerHashtag.isIncluded())
                .map(answerHashtag -> answerHashtag.getHashtag().getName())
                .toList();

        int total = answerHashtags.size();
        double ratio = total == 0 ? 0.0 : (double) covered.size() / total;

        return new InterviewKeywordResultResponse(covered, missing, ratio);
    }

    /**
     * 실전 제출 요청에서 follow-up API로 전달할 누적 interview_history를 구성합니다.
     */
    private List<InterviewHistoryRequest> resolveRealHistoryForFollowUp(
            InterviewSession session,
            String answerText
    ) {
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
    private void syncSessionHistoryFromRequest(InterviewSession session, List<InterviewHistoryRequest> requestHistory) {
        if (requestHistory == null || requestHistory.isEmpty()) {
            throw new InterviewSessionInvalidInputException(ERROR_INTERVIEW_HISTORY_REQUIRED);
        }

        int existingCount = session.getInterviewHistoryView().size();
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
            session.appendHistory(new InterviewHistoryItem(
                    null,
                    turn.question(),
                    turnCategory,
                    turn.answerText(),
                    turnType,
                    turnOrder,
                    turn.topicId()
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

    /**
     * 요청 누적 이력을 반영했을 때 최대 turn 수를 초과하는지 사전 검증합니다.
     */
    private void validateRealTurnLimit(InterviewSession session, List<InterviewHistoryRequest> requestHistory) {
        int existingCount = session.getInterviewHistoryView().size();
        int maxTurnOrder = existingCount - 1;
        for (InterviewHistoryRequest turn : requestHistory) {
            maxTurnOrder = Math.max(maxTurnOrder, turn.turnOrder());
        }
        int finalTurnCount = maxTurnOrder + 1;
        if (finalTurnCount > REAL_MAX_TURN) {
            throw new InterviewSessionInvalidStateException(ERROR_REAL_MAX_TURN_REACHED);
        }
    }

    /**
     * 저장된 세션 이력 아이템을 AI history 요청 포맷으로 변환합니다.
     */
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

    /**
     * history turn_type을 내부 enum으로 파싱합니다.
     */
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

    /**
     * 실전 모드 인터뷰 종료 조건을 통합 평가합니다.
     */
    private boolean shouldEndRealInterview(
            InterviewSession session,
            SessionFollowUpOrchestrator.FollowUpDecision followUpDecision
    ) {
        return TurnType.SESSION_END.wireValue().equalsIgnoreCase(followUpDecision.nextTurnType())
                || followUpDecision.shouldEnd()
                || session.getTurnCount() >= REAL_MAX_TURN;
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
     * 실전 turn 제출 응답 모델을 생성합니다.
     */
    private InterviewRealSubmitResponse toRealTurnResponse(
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
     * 실전 제출 응답용 next_question 모델을 생성합니다.
     */
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
        throw new InterviewSessionInvalidStateException(ERROR_AI_FOLLOW_UP_QUESTION_EMPTY);
    }

    /**
     * real 모드에서 현재 질문 snapshot에 DB 질문 ID가 없을 수 있어, 영속/통계용 질문 앵커를 선택합니다.
     */
    private Question resolvePersistenceQuestionForReal(InterviewQuestionSnapshot snapshot, QuestionType questionType) {
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

    /**
     * 세션 최종 피드백 응답 형태로 변환합니다.
     */
    private InterviewFeedbackDataResponse toFinalSessionFeedbackResponse(
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
    private InterviewSessionFinalFeedbackResponse toSessionFinalFeedbackResponse(
            InterviewFeedbackDataResponse feedback,
            AnswerType interviewType
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
                feedback.badCaseFeedback(),
                metrics,
                feedback.keywordResult(),
                feedback.topicsFeedback(),
                feedback.overallFeedback()
        );
    }

    /**
     * 연습 모드는 단일 turn만 허용하므로 중복 제출을 차단합니다.
     */
    private void validatePracticeSubmissionAvailable(InterviewSession session) {
        if (session.getTurnCount() > 0 || session.getNextTurnType() == TurnType.SESSION_END) {
            throw new InterviewSessionInvalidStateException(ERROR_PRACTICE_ALREADY_SUBMITTED);
        }
    }

    /**
     * 인터뷰 유형별 최종 피드백 요청 가능 상태를 검증합니다.
     */
    private void validateSessionReadyForFinalFeedback(InterviewSession session) {
        if (session.getInterviewType() == AnswerType.REAL_INTERVIEW) {
            validateRealInterviewEnded(session);
            return;
        }
        validatePracticeFinalFeedbackReady(session);
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
        return resolvePersistenceQuestionForReal(session.getCurrentQuestion(), session.getQuestionType());
    }

    /**
     * 세션 인터뷰 유형과 요청 경로의 인터뷰 유형 일치 여부를 검증합니다.
     */
    private void validateSessionType(InterviewSession session, AnswerType answerType) {
        if (session.getInterviewType() != answerType) {
            throw new InterviewSessionInvalidStateException(
                    String.format(ERROR_SESSION_TYPE_MISMATCH_TEMPLATE, answerType, session.getInterviewType())
            );
        }
    }

    /**
     * 제출 가능한 세션 상태인지 검증합니다.
     */
    private void validateSessionAvailable(InterviewSession session) {
        if (session.getStatus() == InterviewSessionStatus.COMPLETED
                || session.getStatus() == InterviewSessionStatus.FAILED
                || session.getStatus() == InterviewSessionStatus.EXPIRED) {
            throw new InterviewSessionInvalidStateException(
                    String.format(ERROR_SESSION_NOT_AVAILABLE_TEMPLATE, session.getStatus())
            );
        }
    }

    /**
     * 실전 제출 가능 여부를 추가 검증합니다.
     * 세션이 이미 종료 응답 상태(session_end)이면 최종 피드백 요청 API를 호출해야 합니다.
     */
    private void validateRealSubmissionAvailable(InterviewSession session) {
        if (session.getNextTurnType() == TurnType.SESSION_END) {
            throw new InterviewSessionInvalidStateException(ERROR_REAL_ALREADY_ENDED);
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

    /**
     * 세션 질문 유형과 실제 질문 유형 일치 여부를 검증합니다.
     */
    private void validateQuestionTypeMatch(QuestionType expected, QuestionType actual) {
        if (expected != actual) {
            throw new InterviewSessionInvalidInputException(
                    String.format(ERROR_QUESTION_TYPE_MISMATCH_TEMPLATE, expected, actual)
            );
        }
    }

    /**
     * 실전 제출 요청에서 답변 텍스트를 추출하고 길이를 검증합니다.
     */
    private String resolveRealAnswerText(RealAnswerSubmitRequest request) {
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
    private void validateOptionalQuestionType(String requestQuestionType, QuestionType sessionQuestionType) {
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
     * 클라이언트 동기화용 마지막 질문 텍스트와 세션 현재 질문이 다르면 경고 로그를 남깁니다.
     */
    private void validateRequestQuestionSync(String requestQuestionText, String currentQuestionText, String sessionId) {
        if (requestQuestionText == null || requestQuestionText.isBlank()) {
            return;
        }
        if (!requestQuestionText.trim().equals(currentQuestionText == null ? null : currentQuestionText.trim())) {
            log.warn("submitReal question text mismatch - sessionId={}, requestQuestion='{}', currentQuestion='{}'",
                    sessionId, requestQuestionText, currentQuestionText);
        }
    }
}
