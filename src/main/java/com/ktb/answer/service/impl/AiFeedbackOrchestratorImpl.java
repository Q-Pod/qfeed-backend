package com.ktb.answer.service.impl;

import com.ktb.ai.feedback.dto.response.AiFeedbackBadCaseFeedback;
import com.ktb.ai.feedback.dto.response.AiFeedbackResponse;
import com.ktb.ai.feedback.dto.response.AiFeedbackFeedback;
import com.ktb.ai.feedback.dto.response.AiFeedbackMetric;
import com.ktb.ai.feedback.dto.response.BadCaseType;
import com.ktb.ai.feedback.service.AiFeedbackService;
import com.ktb.answer.domain.Answer;
import com.ktb.answer.dto.FeedbackStatus;
import com.ktb.answer.dto.response.feedback.FeedbackResponse;
import com.ktb.answer.dto.response.common.MetricScore;
import com.ktb.answer.exception.AnswerNotFoundException;
import com.ktb.answer.repository.AnswerRepository;
import com.ktb.answer.service.AiFeedbackOrchestrator;
import com.ktb.answer.service.AnswerDomainService;
import com.ktb.auth.domain.UserAccount;
import com.ktb.common.dto.ApiResponse;
import com.ktb.metric.domain.AnswerMetric;
import com.ktb.metric.domain.Metric;
import com.ktb.metric.repository.AnswerMetricRepository;
import com.ktb.metric.repository.MetricRepository;
import com.ktb.question.domain.Question;
import java.util.ArrayList;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiFeedbackOrchestratorImpl implements AiFeedbackOrchestrator {

    private final AnswerRepository answerRepository;
    private final AnswerDomainService answerDomainService;
    private final AiFeedbackService aiFeedbackService;
    private final MetricRepository metricRepository;
    private final AnswerMetricRepository answerMetricRepository;

    @Override
    @Transactional
    public FeedbackResponse getFeedbackSync(Long answerId, Long accountId) {
        log.info("Getting AI feedback synchronously for answerId: {}", answerId);

        Answer answer = answerRepository.findById(answerId)
            .orElseThrow(() -> new AnswerNotFoundException(answerId));

        Question question = answer.getQuestion();
        UserAccount account = answer.getAccount();

        answerDomainService.validateOwnership(answer, accountId);

        log.debug("Answer found - answerId: {}, questionId: {}, userId: {}",
                answerId, question.getId(), account.getId());

        ApiResponse<AiFeedbackResponse> apiResponse = getAiFeedback(account, question, answer);

        FeedbackResponse response = isBadCase(apiResponse)
                ? handleBadCaseResponse(answer, apiResponse)
                : handleSuccessResponse(answer, apiResponse);

        log.info("AI feedback retrieved successfully for answerId: {}", answerId);

        return response;
    }

    @Override
    public void enqueue(Long answerId) {
        // TODO: MVP V2 구현 필요
        // 1. Kafka Producer 구성
        // 2. ANSWER_AI_FEEDBACK_REQUESTED 이벤트 발행
        // 3. 이벤트 payload: { answerId, questionId, answerContent }
        // 4. 발행 실패 시 예외 처리 (재시도 정책)

        log.info("Enqueueing AI feedback request for answerId: {}", answerId);

        // TODO: Kafka 연동 후 활성화
        // kafkaTemplate.send("answer-feedback-requests", answerId.toString(), event);

        log.warn("AI feedback enqueue not implemented yet (Kafka integration required)");
    }

    @Override
    public FeedbackStatus getStatus(Long answerId) {
        // TODO: 구현 필요
        // 1. Answer 조회
        // 2. Answer.status를 FeedbackStatus로 매핑
        //    - AI_FEEDBACK_PROCESSING -> PROCESSING
        //    - COMPLETED -> COMPLETED
        //    - FAILED -> FAILED
        //    - FAILED_RETRYABLE -> FAILED_RETRYABLE
        // 3. FeedbackStatus 반환

        log.debug("Getting feedback status for answerId: {}", answerId);

        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new AnswerNotFoundException(answerId));

        return FeedbackStatus.from(answer.getStatus());
    }

    @Override
    public void requestRetry(Long answerId) {
        // TODO: MVP V2 구현 필요
        // 1. Answer 조회
        // 2. 상태 검증 (FAILED_RETRYABLE만 허용)
        // 3. 재시도 횟수 체크 (최대 3회)
        // 4. Kafka 이벤트 재발행
        // 5. Answer 상태를 AI_FEEDBACK_PROCESSING으로 전이

        log.info("Requesting feedback retry for answerId: {}", answerId);

        // TODO: 재시도 정책 구현 후 활성화
        throw new UnsupportedOperationException("Feedback retry not yet implemented");
    }

    private ApiResponse<AiFeedbackResponse> getAiFeedback(
        UserAccount account, Question question, Answer answer
        ) {

        return aiFeedbackService.evaluateSync(
            account.getId(),
            question.getId(),
            question.getType(),
            question.getCategory(),
            answer.getType(),
            question.getContent(),
            answer.getContent()
        );
    }

    private boolean isBadCase(ApiResponse<AiFeedbackResponse> response) {
        return "bad_case_detected".equalsIgnoreCase(response.message());
    }

    private FeedbackResponse handleBadCaseResponse(Answer answer, ApiResponse<AiFeedbackResponse> apiResponse) {
        AiFeedbackBadCaseFeedback badCase = apiResponse.data().badCaseFeedback();
        BadCaseType badCaseType = badCase.getTypeEnum();

        String failureMessage = badCaseType.getMessage() + "\n\n" + badCaseType.getGuidance();

        answer.setAiFeedback(badCaseType.getGuidance());
        answerRepository.save(answer);

        log.warn("Bad case detected for answerId: {}, type: {}", answer.getId(), badCaseType);

        return FeedbackResponse.failed(failureMessage);
    }

    private FeedbackResponse handleSuccessResponse(Answer answer, ApiResponse<AiFeedbackResponse> apiResponse) {
        AiFeedbackResponse data = apiResponse.data();

        saveAnswerMetrics(answer, data.metrics());

        List<MetricScore> radarChart = convertToRadarChart(data.metrics());
        String combinedFeedback = combineFeedback(data.feedback());

        answer.setAiFeedback(combinedFeedback);
        answerRepository.save(answer);

        return FeedbackResponse.completed(combinedFeedback, radarChart);
    }

    private List<MetricScore> convertToRadarChart(List<AiFeedbackMetric> metrics) {
        if (metrics == null) {
            return null;
        }

        return metrics.stream()
                .map(metric -> new MetricScore(
                        metric.name(),        // metricName
                        metric.comment(),     // metricDescription
                        metric.score(),       // score (1~5)
                        5                     // maxScore
                ))
                .collect(Collectors.toList());
    }

    private void saveAnswerMetrics(Answer answer, List<AiFeedbackMetric> aiMetrics) {
        if (aiMetrics == null || aiMetrics.isEmpty()) {
            return;
        }

        Map<String, Metric> metricMap =
            metricRepository.findAllByNameIn(aiMetrics.stream().map(AiFeedbackMetric::name).toList())
                .stream()
                .collect(Collectors.toMap(Metric::getName, m -> m));

        List<AnswerMetric> answerMetrics = new ArrayList<>(aiMetrics.size());

        for (AiFeedbackMetric aiMetric : aiMetrics) {
            Metric metric = metricMap.computeIfAbsent(aiMetric.name(), name ->
                metricRepository.save(Metric.create(name, ""))
            );
            int score = aiMetric.score() == null ? 1 : aiMetric.score();
            answerMetrics.add(AnswerMetric.create(answer, metric, score));
        }

        answerMetricRepository.saveAll(answerMetrics);
    }

    private String combineFeedback(AiFeedbackFeedback feedback) {
        if (feedback == null) {
            return null;
        }

        return feedback.strengths() + "\n\n" + feedback.improvements();
    }
}
