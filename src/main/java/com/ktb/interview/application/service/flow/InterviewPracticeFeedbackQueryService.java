package com.ktb.interview.application.service.flow;

import com.ktb.answer.domain.Answer;
import com.ktb.answer.exception.AnswerNotFoundException;
import com.ktb.answer.repository.AnswerRepository;
import com.ktb.answer.service.AnswerDomainService;
import com.ktb.hashtag.domain.AnswerHashtag;
import com.ktb.metric.domain.AnswerMetric;
import com.ktb.metric.repository.AnswerMetricRepository;
import com.ktb.hashtag.repository.AnswerHashtagRepository;
import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.dto.ai.InterviewFeedbackMetricResponse;
import com.ktb.interview.dto.ai.InterviewKeywordResultResponse;
import com.ktb.interview.dto.ai.InterviewOverallFeedbackResponse;
import com.ktb.interview.session.domain.InterviewSessionStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 연습 모드 피드백 조회 플로우를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewPracticeFeedbackQueryService {

    private static final String SESSION_STATUS_COMPLETED = InterviewSessionStatus.COMPLETED.name();

    private final AnswerRepository answerRepository;
    private final AnswerDomainService answerDomainService;
    private final AnswerMetricRepository answerMetricRepository;
    private final AnswerHashtagRepository answerHashtagRepository;

    /**
     * 연습 모드 답변 ID로 저장된 피드백/메트릭/키워드 결과를 조회합니다.
     */
    @Transactional(readOnly = true)
    public InterviewFeedbackDataResponse getPracticeFeedback(Long accountId, Long answerId) {
        log.info("getPracticeFeedback flow start - accountId={}, answerId={}", accountId, answerId);
        Answer answer = answerRepository.findByIdWithQuestion(answerId);
        if (answer == null) {
            log.warn("getPracticeFeedback answer not found - answerId={}", answerId);
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
        log.info("getPracticeFeedback flow success - accountId={}, answerId={}, metricCount={}, hashtagCount={}",
                accountId, answerId, metricResponses.size(), answerHashtags.size());

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
}
