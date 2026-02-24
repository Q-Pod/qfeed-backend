package com.ktb.interview.session.persistence.adapter;

import com.ktb.answer.domain.TurnType;
import com.ktb.interview.dto.ai.InterviewBadCaseFeedbackResponse;
import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.dto.ai.InterviewFeedbackMetricResponse;
import com.ktb.interview.dto.ai.InterviewKeywordResultResponse;
import com.ktb.interview.dto.ai.InterviewOverallFeedbackResponse;
import com.ktb.interview.dto.ai.InterviewTopicFeedbackResponse;
import com.ktb.interview.port.out.InterviewSessionFinalFeedbackStore;
import com.ktb.interview.session.domain.InterviewHistoryItem;
import com.ktb.interview.session.domain.InterviewQuestionSnapshot;
import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.exception.InterviewSessionInvalidStateException;
import com.ktb.interview.session.persistence.entity.InterviewSessionEntity;
import com.ktb.interview.session.persistence.entity.InterviewSessionFeedbackEntity;
import com.ktb.interview.session.persistence.entity.InterviewSessionMetricEntity;
import com.ktb.interview.session.persistence.entity.InterviewTopicFeedbackEntity;
import com.ktb.interview.session.persistence.entity.InterviewTurnEntity;
import com.ktb.interview.session.persistence.repository.InterviewSessionEntityRepository;
import com.ktb.interview.session.persistence.repository.InterviewSessionFeedbackEntityRepository;
import com.ktb.interview.session.persistence.repository.InterviewSessionMetricEntityRepository;
import com.ktb.interview.session.persistence.repository.InterviewTopicFeedbackEntityRepository;
import com.ktb.interview.session.persistence.repository.InterviewTurnEntityRepository;
import com.ktb.metric.domain.Metric;
import com.ktb.metric.repository.MetricRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 최종 피드백 응답 수신 이후 세션 정규화 테이블에 결과를 저장하는 구현체.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JpaInterviewSessionFinalFeedbackStore implements InterviewSessionFinalFeedbackStore {

    private static final String ERROR_INITIAL_QUESTION_ID_REQUIRED =
            "initial question id is required for interview_session persistence";
    private static final int KEYWORD_COVERAGE_SCALE = 1;

    private final InterviewSessionEntityRepository interviewSessionEntityRepository;
    private final InterviewTurnEntityRepository interviewTurnEntityRepository;
    private final InterviewSessionFeedbackEntityRepository interviewSessionFeedbackEntityRepository;
    private final InterviewSessionMetricEntityRepository interviewSessionMetricEntityRepository;
    private final InterviewTopicFeedbackEntityRepository interviewTopicFeedbackEntityRepository;
    private final MetricRepository metricRepository;

    /**
     * 최종 피드백 1건 기준으로 세션/턴/피드백/메트릭/토픽 피드백을 재구성 저장합니다.
     */
    @Override
    @Transactional
    public void persistFinalFeedback(InterviewSession session, InterviewFeedbackDataResponse feedback) {
        String sessionId = session.getSessionId();
        Long initialQuestionId = resolveInitialQuestionId(session, feedback);
        log.info("persistFinalFeedback start - sessionId={}, turnCount={}", sessionId, session.getTurnCount());

        upsertSession(session, initialQuestionId);
        replaceTurns(session);
        upsertSessionFeedback(sessionId, feedback);
        replaceSessionMetrics(sessionId, feedback.metrics());
        replaceTopicFeedback(sessionId, feedback.topicsFeedback());

        log.info("persistFinalFeedback completed - sessionId={}, turnCount={}, metrics={}, topics={}",
                sessionId,
                session.getTurnCount(),
                feedback.metrics() == null ? 0 : feedback.metrics().size(),
                feedback.topicsFeedback() == null ? 0 : feedback.topicsFeedback().size());
    }

    /**
     * 세션의 초기 질문 앵커(question_id)를 확정합니다.
     * 우선순위: session.initialQuestionId > history.questionId > feedback.questionId
     */
    private Long resolveInitialQuestionId(InterviewSession session, InterviewFeedbackDataResponse feedback) {
        if (session.getInitialQuestionId() != null) {
            return session.getInitialQuestionId();
        }

        Long fromHistory = session.getInterviewHistoryView().stream()
                .map(InterviewHistoryItem::questionId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (fromHistory != null) {
            return fromHistory;
        }

        if (feedback != null && feedback.questionId() != null) {
            return feedback.questionId();
        }

        throw new InterviewSessionInvalidStateException(ERROR_INITIAL_QUESTION_ID_REQUIRED);
    }

    /**
     * interview_session 레코드를 생성 또는 갱신합니다.
     */
    private void upsertSession(InterviewSession session, Long initialQuestionId) {
        String sessionId = session.getSessionId();
        InterviewSessionEntity entity = interviewSessionEntityRepository.findById(sessionId)
                .orElseGet(() -> InterviewSessionEntity.create(sessionId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startedAt = session.getStartedAt() == null ? now : session.getStartedAt();
        LocalDateTime updatedAt = session.getUpdatedAt() == null ? now : session.getUpdatedAt();
        LocalDateTime expiresAt = session.getExpiresAt() == null ? now : session.getExpiresAt();
        LocalDateTime createdAt = entity.getCreatedAt() == null ? startedAt : entity.getCreatedAt();

        entity.sync(
                session.getAccountId(),
                session.getInterviewType().name(),
                session.getQuestionType().name(),
                session.getStatus().name(),
                initialQuestionId,
                startedAt,
                session.getEndedAt(),
                expiresAt,
                createdAt,
                updatedAt
        );
        interviewSessionEntityRepository.save(entity);
        log.debug("persistFinalFeedback upsertSession - sessionId={}, status={}, initialQuestionId={}",
                sessionId, session.getStatus(), initialQuestionId);
    }

    /**
     * 세션 히스토리를 interview_turn으로 전체 재적재(기존 삭제 후 insert)합니다.
     */
    private void replaceTurns(InterviewSession session) {
        String sessionId = session.getSessionId();
        interviewTurnEntityRepository.deleteByIdSessionId(sessionId);

        List<InterviewHistoryItem> history = session.getInterviewHistoryView();
        if (history.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime defaultCreatedAt = session.getStartedAt() == null ? now : session.getStartedAt();
        LocalDateTime defaultUpdatedAt = session.getUpdatedAt() == null ? now : session.getUpdatedAt();
        String currentCategory = resolveCurrentCategory(session.getCurrentQuestion());

        Map<Integer, String> topicCategoryMap = buildTopicCategoryMap(history, currentCategory);
        List<InterviewTurnEntity> turnEntities = new ArrayList<>(history.size());
        for (InterviewHistoryItem item : history) {
            String category = resolveTurnCategory(item, topicCategoryMap, currentCategory);
            if (item.topicId() != null && category != null) {
                topicCategoryMap.putIfAbsent(item.topicId(), category);
            }

            turnEntities.add(InterviewTurnEntity.create(
                    sessionId,
                    item.turnOrder(),
                    item.topicId(),
                    toPersistedTurnType(item.turnType()),
                    category,
                    item.question(),
                    item.answerText(),
                    defaultCreatedAt.plusSeconds(Math.max(item.turnOrder(), 0)),
                    defaultUpdatedAt
            ));
        }
        // TODO : 배치 insert 적용 검토해보기
        interviewTurnEntityRepository.saveAll(turnEntities);
        log.debug("persistFinalFeedback replaceTurns - sessionId={}, savedTurns={}", sessionId, turnEntities.size());
    }

    /**
     * 토픽별 category fallback 계산을 위한 맵을 생성합니다.
     */
    private Map<Integer, String> buildTopicCategoryMap(List<InterviewHistoryItem> history, String fallbackCategory) {
        Map<Integer, String> map = new HashMap<>();
        for (InterviewHistoryItem item : history) {
            if (item.topicId() != null && item.category() != null) {
                map.putIfAbsent(item.topicId(), item.category().name());
            }
        }

        if (fallbackCategory != null) {
            for (InterviewHistoryItem item : history) {
                if (item.topicId() != null) {
                    map.putIfAbsent(item.topicId(), fallbackCategory);
                }
            }
        }
        return map;
    }

    /**
     * turn 저장 시 category를 결정합니다.
     * 우선순위: turn.category > topic별 맵 > 현재 질문 category
     */
    private String resolveTurnCategory(
            InterviewHistoryItem item,
            Map<Integer, String> topicCategoryMap,
            String fallbackCategory
    ) {
        if (item.category() != null) {
            return item.category().name();
        }
        if (item.topicId() != null && topicCategoryMap.containsKey(item.topicId())) {
            return topicCategoryMap.get(item.topicId());
        }
        return fallbackCategory;
    }

    private String resolveCurrentCategory(InterviewQuestionSnapshot currentQuestion) {
        if (currentQuestion == null || currentQuestion.category() == null) {
            return null;
        }
        return currentQuestion.category().name();
    }

    /**
     * 저장 turn_type은 new_topic/follow_up 두 값으로만 정규화합니다.
     */
    private String toPersistedTurnType(TurnType turnType) {
        if (turnType == null) {
            return TurnType.FOLLOW_UP.wireValue();
        }
        if (turnType == TurnType.MAIN || turnType == TurnType.NEW_TOPIC) {
            return TurnType.NEW_TOPIC.wireValue();
        }
        return TurnType.FOLLOW_UP.wireValue();
    }

    /**
     * interview_session_feedback 레코드를 생성 또는 갱신합니다.
     */
    private void upsertSessionFeedback(String sessionId, InterviewFeedbackDataResponse feedback) {
        InterviewSessionFeedbackEntity entity = interviewSessionFeedbackEntityRepository.findById(sessionId)
                .orElseGet(() -> InterviewSessionFeedbackEntity.create(sessionId));

        InterviewBadCaseFeedbackResponse badCase = feedback.badCaseFeedback();
        InterviewKeywordResultResponse keywordResult = feedback.keywordResult();
        InterviewOverallFeedbackResponse overall = feedback.overallFeedback();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = entity.getCreatedAt() == null ? now : entity.getCreatedAt();

        entity.sync(
                badCase == null ? null : badCase.type(),
                badCase == null ? null : badCase.message(),
                badCase == null ? null : badCase.guidance(),
                keywordResult == null ? null : keywordResult.coveredKeywords(),
                keywordResult == null ? null : keywordResult.missingKeywords(),
                toScaledCoverageRatio(keywordResult == null ? null : keywordResult.coverageRatio()),
                overall == null ? null : overall.strengths(),
                overall == null ? null : overall.improvements(),
                createdAt,
                now
        );
        interviewSessionFeedbackEntityRepository.save(entity);
        log.debug("persistFinalFeedback upsertSessionFeedback - sessionId={}, hasBadCase={}, hasOverall={}",
                sessionId, badCase != null, overall != null);
    }

    /**
     * 키워드 커버리지 비율을 저장 스케일(소수 1자리)로 정규화합니다.
     */
    private BigDecimal toScaledCoverageRatio(Double coverageRatio) {
        if (coverageRatio == null) {
            return null;
        }
        return BigDecimal.valueOf(coverageRatio)
                .setScale(KEYWORD_COVERAGE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 세션 메트릭을 전체 재적재(기존 삭제 후 insert)합니다.
     */
    private void replaceSessionMetrics(String sessionId, List<InterviewFeedbackMetricResponse> metrics) {
        interviewSessionMetricEntityRepository.deleteByIdSessionId(sessionId);
        if (metrics == null || metrics.isEmpty()) {
            log.debug("persistFinalFeedback replaceSessionMetrics skipped - sessionId={}, reason=empty", sessionId);
            return;
        }

        Map<String, Metric> metricMap = resolveMetricMap(metrics);
        LocalDateTime now = LocalDateTime.now();
        List<InterviewSessionMetricEntity> entities = new ArrayList<>();

        for (InterviewFeedbackMetricResponse metric : metrics) {
            if (metric.name() == null || metric.name().isBlank()) {
                continue;
            }
            Metric persistedMetric = metricMap.get(metric.name());
            if (persistedMetric == null) {
                continue;
            }

            Integer score = metric.score();
            entities.add(InterviewSessionMetricEntity.create(
                    sessionId,
                    persistedMetric.getId(),
                    score,
                    metric.comment(),
                    now,
                    now
            ));
        }
        if (!entities.isEmpty()) {
            interviewSessionMetricEntityRepository.saveAll(entities);
        }
        log.debug("persistFinalFeedback replaceSessionMetrics - sessionId={}, savedMetrics={}",
                sessionId, entities.size());
    }

    /**
     * metric name 목록을 기존 metric 테이블과 동기화하고 id 맵을 반환합니다.
     */
    private Map<String, Metric> resolveMetricMap(List<InterviewFeedbackMetricResponse> metrics) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (InterviewFeedbackMetricResponse metric : metrics) {
            if (metric.name() != null && !metric.name().isBlank()) {
                names.add(metric.name());
            }
        }

        Map<String, Metric> metricMap = new HashMap<>();
        if (names.isEmpty()) {
            return metricMap;
        }

        metricRepository.findAllByNameIn(List.copyOf(names))
                .forEach(metric -> metricMap.put(metric.getName(), metric));

        for (String name : names) {
            if (metricMap.containsKey(name)) {
                continue;
            }
            Metric created = metricRepository.save(Metric.create(name, ""));
            metricMap.put(name, created);
        }
        return metricMap;
    }

    /**
     * 토픽별 피드백을 전체 재적재(기존 삭제 후 insert)합니다.
     */
    private void replaceTopicFeedback(String sessionId, List<InterviewTopicFeedbackResponse> topicsFeedback) {
        interviewTopicFeedbackEntityRepository.deleteByIdSessionId(sessionId);
        if (topicsFeedback == null || topicsFeedback.isEmpty()) {
            log.debug("persistFinalFeedback replaceTopicFeedback skipped - sessionId={}, reason=empty", sessionId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<InterviewTopicFeedbackEntity> entities = new ArrayList<>();
        for (InterviewTopicFeedbackResponse topic : topicsFeedback) {
            if (topic.topicId() == null) {
                continue;
            }
            entities.add(InterviewTopicFeedbackEntity.create(
                    sessionId,
                    topic.topicId(),
                    topic.mainQuestion(),
                    topic.strengths(),
                    topic.improvements(),
                    now,
                    now
            ));
        }

        if (!entities.isEmpty()) {
            interviewTopicFeedbackEntityRepository.saveAll(entities);
        }
        log.debug("persistFinalFeedback replaceTopicFeedback - sessionId={}, savedTopics={}",
                sessionId, entities.size());
    }
}
