package com.ktb.interview.session.persistence;

import com.ktb.async.contract.FeedbackCompletedEvent;
import com.ktb.answer.domain.Answer;
import com.ktb.answer.repository.AnswerRepository;
import com.ktb.hashtag.domain.AnswerHashtag;
import com.ktb.hashtag.domain.Hashtag;
import com.ktb.hashtag.domain.QuestionHashtag;
import com.ktb.hashtag.repository.AnswerHashtagRepository;
import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.dto.ai.InterviewFeedbackMetricResponse;
import com.ktb.interview.dto.ai.InterviewKeywordResultResponse;
import com.ktb.interview.dto.ai.InterviewOverallFeedbackResponse;
import com.ktb.metric.domain.AnswerMetric;
import com.ktb.metric.domain.Metric;
import com.ktb.metric.repository.AnswerMetricRepository;
import com.ktb.metric.repository.MetricRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 최종 AI 피드백 결과를 Answer/Metric/Hashtag 저장소에 영속화하는 구현체입니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewFinalFeedbackPersistenceServiceImpl implements InterviewFinalFeedbackPersistenceService {

    private final AnswerRepository answerRepository;
    private final MetricRepository metricRepository;
    private final AnswerMetricRepository answerMetricRepository;
    private final AnswerHashtagRepository answerHashtagRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * AI 피드백 응답을 Answer/Metric/Hashtag 저장 구조로 분해해 영속화합니다.
     */
    @Override
    @Transactional
    public void persistAnswerFeedback(
            Answer answer,
            InterviewFeedbackDataResponse feedback,
            List<QuestionHashtag> questionHashtags
    ) {
        log.info("persistAnswerFeedback start - answerId={}, sessionId={}, metricCount={}, hashtagCount={}",
                answer.getId(),
                answer.getSessionId(),
                feedback.metrics() == null ? 0 : feedback.metrics().size(),
                questionHashtags == null ? 0 : questionHashtags.size());
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

        Map<String, Integer> metricsMap = metrics.stream()
                .collect(Collectors.toMap(
                        InterviewFeedbackMetricResponse::name,
                        m -> m.score() == null ? 1 : m.score()
                ));
        String strengths = overall != null ? overall.strengths() : null;
        String improvements = overall != null ? overall.improvements() : null;
        eventPublisher.publishEvent(FeedbackCompletedEvent.create(
                answer, metricsMap, null, strengths, improvements, null, null));
        log.info("persistAnswerFeedback success - answerId={}, sessionId={}", answer.getId(), answer.getSessionId());
    }

    /**
     * AI 메트릭을 Metric, AnswerMetric 엔티티로 매핑해 저장합니다.
     */
    private void saveMetrics(Answer answer, List<InterviewFeedbackMetricResponse> metrics) {
        if (metrics.isEmpty()) {
            log.debug("saveMetrics skipped - answerId={}, reason=empty", answer.getId());
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
        log.debug("saveMetrics persisted - answerId={}, count={}", answer.getId(), answerMetrics.size());
    }

    /**
     * 질문 키워드 목록과 AI 커버 키워드 결과를 비교해 AnswerHashtag를 저장합니다.
     */
    private void saveKeywordResult(
            Answer answer,
            List<QuestionHashtag> questionHashtags,
            InterviewKeywordResultResponse keywordResult
    ) {
        if (questionHashtags == null || questionHashtags.isEmpty()) {
            log.debug("saveKeywordResult skipped - answerId={}, reason=no-question-hashtag", answer.getId());
            return;
        }
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
        log.debug("saveKeywordResult persisted - answerId={}, count={}", answer.getId(), answerHashtags.size());
    }
}
