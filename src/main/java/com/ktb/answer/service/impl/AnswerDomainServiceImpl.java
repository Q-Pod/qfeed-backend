package com.ktb.answer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ktb.answer.domain.Answer;
import com.ktb.answer.domain.AnswerStatus;
import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.dto.AiFeedbackSummary;
import com.ktb.answer.dto.AnswerContentResult;
import com.ktb.answer.dto.AnswerDetailQuery;
import com.ktb.answer.dto.AnswerDetailResult;
import com.ktb.answer.dto.AnswerListCursor;
import com.ktb.answer.dto.FeedbackStatus;
import com.ktb.answer.dto.ImmediateFeedbackResult;
import com.ktb.answer.dto.KeywordCheckResult;
import com.ktb.answer.dto.QuestionSummary;
import com.ktb.answer.dto.response.detail.AnswerQuestionInfo;
import com.ktb.answer.dto.response.list.AnswerListResponse;
import com.ktb.answer.dto.response.list.AnswerSummary;
import com.ktb.answer.dto.response.list.FeedbackInfo;
import com.ktb.answer.dto.response.list.PaginationInfo;
import com.ktb.answer.exception.AnswerAccessDeniedException;
import com.ktb.answer.exception.AnswerInvalidContentException;
import com.ktb.answer.exception.AnswerListInvalidInputException;
import com.ktb.answer.exception.AnswerNotFoundException;
import com.ktb.answer.exception.DuplicateAnswerException;
import com.ktb.answer.exception.InvalidAnswerContentException;
import com.ktb.answer.exception.InvalidAnswerStatusTransitionException;
import com.ktb.answer.repository.AnswerRepository;
import com.ktb.answer.service.AnswerDomainService;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.exception.account.AccountNotFoundException;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.hashtag.domain.AnswerHashtag;
import com.ktb.hashtag.repository.AnswerHashtagRepository;
import com.ktb.metric.domain.AnswerMetric;
import com.ktb.metric.repository.AnswerMetricRepository;
import com.ktb.question.domain.Question;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import com.ktb.question.exception.QuestionNotFoundException;
import com.ktb.question.repository.QuestionRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerDomainServiceImpl implements AnswerDomainService {

    private static final int MAX_ANSWER_CONTENT_LENGTH = 1_500;

    private final QuestionRepository questionRepository;
    private final UserAccountRepository userAccountRepository;
    private final AnswerRepository answerRepository;
    private final AnswerHashtagRepository answerHashtagRepository;
    private final AnswerMetricRepository answerMetricRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @Override
    public Answer createAnswer(Long accountId, Long questionId, String answerContent, AnswerType type) {
        log.debug("Creating answer for accountId: {}, questionId: {}", accountId, questionId);

        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        return Answer.create(question, account, answerContent, type);
    }

    @Override
    public void validateOwnership(Answer answer, Long accountId) throws AnswerAccessDeniedException {
        log.debug("Validating ownership for answerId: {}, accountId: {}", answer.getId(), accountId);

        if (!answer.isOwnedBy(accountId)) {
            throw new AnswerAccessDeniedException(answer.getId(), accountId);
        }
    }

    @Override
    public void transitionStatus(Answer answer, AnswerStatus nextStatus) throws InvalidAnswerStatusTransitionException {
        log.debug("Transitioning answer status: answerId={}, from={}, to={}",
                answer.getId(), answer.getStatus(), nextStatus);

        answer.transitionTo(nextStatus);
    }

    @Override
    public void checkDuplicateAnswer(String sessionId, Long questionId) throws DuplicateAnswerException {
        log.debug("Checking duplicate answer for sessionId: {}, questionId: {}", sessionId, questionId);
        // TODO: ANSWER_SESSION 엔티티 구현 후 활성화
    }

    @Override
    public void validateAnswerContent(String answerText) throws InvalidAnswerContentException {
        log.debug("Validating answer content: hasText={}", answerText != null && !answerText.isBlank());

        boolean hasText = answerText != null && !answerText.isBlank();
        if (!hasText || answerText.length() > MAX_ANSWER_CONTENT_LENGTH) {
            throw new AnswerInvalidContentException();
        }
    }

    @Override
    public AnswerListResponse getList(
            Long accountId,
            AnswerType type,
            QuestionCategory category,
            QuestionType questionType,
            LocalDate dateFrom,
            LocalDate dateTo,
            String cursor,
            Integer limit
    ) {
        validateQuestionType(questionType);
        LocalDateRange dateRange = resolveDateRange(dateFrom, dateTo);
        int resolvedLimit = resolveLimit(limit);
        AnswerListCursor cursorPayload = decodeCursor(cursor);
        PageRequest pageRequest = PageRequest.of(0, resolvedLimit);

        LocalDateTime from = dateRange.start().atStartOfDay();
        LocalDateTime to = dateRange.end().atTime(LocalTime.MAX);

        Slice<Answer> answers = cursorPayload == null
                ? answerRepository.findByAccountIdWithFiltersNoCursor(
                accountId,
                type,
                category,
                questionType,
                from,
                to,
                pageRequest
        )
                : answerRepository.findByAccountIdWithFilters(
                accountId,
                type,
                category,
                questionType,
                from,
                to,
                cursorPayload.lastCreatedAt(),
                cursorPayload.lastAnswerId(),
                pageRequest
        );

        return toAnswerListResponse(answers, resolvedLimit);
    }

    @Override
    public AnswerDetailResult getDetail(Long accountId, Long answerId, AnswerDetailQuery query) {
        Answer answer = answerRepository.findByIdWithQuestion(answerId);
        if (answer == null) {
            throw new AnswerNotFoundException(answerId);
        }
        validateOwnership(answer, accountId);

        AnswerContentResult answerContent = new AnswerContentResult(
                answer.getContent(),
                null,
                null,
                answer.getCreatedAt() == null ? null : answer.getCreatedAt().toString()
        );

        QuestionSummary questionSummary = null;
        if (query.includeQuestion()) {
            Question question = answer.getQuestion();
            questionSummary = new QuestionSummary(
                    question.getId(),
                    question.getContent(),
                    question.getCategory().name(),
                    question.getType().name()
            );
        }

        ImmediateFeedbackResult immediateFeedback = null;
        if (query.includeImmediateFeedback()) {
            immediateFeedback = loadImmediateFeedback(answerId);
        }

        AiFeedbackSummary aiFeedback = null;
        if (query.includeFeedback()) {
            aiFeedback = loadAiFeedback(answer);
        }

        return new AnswerDetailResult(
                answer.getId(),
                answer.getStatus(),
                answer.getType(),
                questionSummary,
                answerContent,
                immediateFeedback,
                aiFeedback
        );
    }

    private ImmediateFeedbackResult loadImmediateFeedback(Long answerId) {
        List<AnswerHashtag> answerHashtags = answerHashtagRepository.findByAnswerIdWithHashtag(answerId);

        List<KeywordCheckResult> keywords = answerHashtags.stream()
                .map(ah -> new KeywordCheckResult(
                        ah.getHashtag().getId(),
                        ah.getHashtag().getName(),
                        ah.isIncluded()
                ))
                .toList();

        return new ImmediateFeedbackResult(keywords);
    }

    private AiFeedbackSummary loadAiFeedback(Answer answer) {
        FeedbackView feedback = buildFeedbackView(answer);
        return new AiFeedbackSummary(
                feedback.status(),
                feedback.metrics(),
                feedback.comment()
        );
    }

    private FeedbackView buildFeedbackView(Answer answer) {
        FeedbackStatus status = FeedbackStatus.from(answer.getStatus());

        if (status != FeedbackStatus.COMPLETED) {
            return new FeedbackView(status, null, null);
        }

        List<AnswerMetric> metrics = answerMetricRepository.findByAnswerIdWithMetric(answer.getId());
        Map<String, Integer> metricsMap = metrics.stream()
                .collect(Collectors.toMap(
                        AnswerMetric::getMetricName,
                        AnswerMetric::getScore
                ));

        return new FeedbackView(status, metricsMap, answer.getAiFeedback());
    }

    private LocalDateRange resolveDateRange(LocalDate dateFrom, LocalDate dateTo) {
        LocalDate resolvedTo = dateTo == null ? LocalDate.now() : dateTo;
        LocalDate resolvedFrom = dateFrom == null ? resolvedTo.minusMonths(1) : dateFrom;

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new AnswerListInvalidInputException("dateFrom must be before or equal to dateTo");
        }

        return new LocalDateRange(resolvedFrom, resolvedTo);
    }

    private int resolveLimit(Integer limit) {
        int resolved = limit == null ? 10 : limit;
        if (resolved < 1 || resolved > 50) {
            throw new AnswerListInvalidInputException("limit must be between 1 and 50");
        }
        return resolved;
    }

    private void validateQuestionType(QuestionType questionType) {
        if (questionType == null) {
            return;
        }

        if (questionType == QuestionType.PORTFOLIO) {
            throw new AnswerListInvalidInputException(
                    "questionType is only supported for [CS, SYSTEM_DESIGN]"
            );
        }
    }

    private AnswerListCursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cursor);
            AnswerListCursor payload = jsonMapper.readValue(decoded, AnswerListCursor.class);
            if (payload.lastCreatedAt() == null || payload.lastAnswerId() == null) {
                throw new AnswerListInvalidInputException("cursor payload is incomplete");
            }
            return payload;
        } catch (IllegalArgumentException | IOException e) {
            throw new AnswerListInvalidInputException("invalid cursor", e);
        }
    }

    private String encodeCursor(AnswerListCursor cursor) {
        try {
            byte[] json = jsonMapper.writeValueAsBytes(cursor);
            return Base64.getEncoder().encodeToString(json);
        } catch (JsonProcessingException e) {
            throw new AnswerListInvalidInputException("failed to encode cursor", e);
        }
    }

    private AnswerListResponse toAnswerListResponse(Slice<Answer> answers, int limit) {
        List<AnswerSummary> records = answers.getContent().stream()
                .map(answer -> new AnswerSummary(
                        answer.getId(),
                        answer.getType().name(),
                        answer.getCreatedAt() == null ? null : answer.getCreatedAt().toString(),
                        new AnswerQuestionInfo(
                                answer.getQuestion().getId(),
                                answer.getQuestion().getContent(),
                                answer.getQuestion().getCategory().name()
                        ),
                        toFeedbackInfo(answer)
                ))
                .toList();

        String nextCursor = null;
        if (answers.hasNext() && !answers.getContent().isEmpty()) {
            Answer last = answers.getContent().getLast();
            nextCursor = encodeCursor(new AnswerListCursor(last.getCreatedAt(), last.getId()));
        }

        PaginationInfo pagination = new PaginationInfo(
                limit,
                answers.hasNext(),
                nextCursor
        );

        return new AnswerListResponse(records, pagination);
    }

    private FeedbackInfo toFeedbackInfo(Answer answer) {
        AnswerStatus status = answer.getStatus();
        boolean available = status == AnswerStatus.COMPLETED;
        return new FeedbackInfo(available, status.name());
    }

    private record LocalDateRange(LocalDate start, LocalDate end) {
    }

    private record FeedbackView(
            FeedbackStatus status,
            Map<String, Integer> metrics,
            String comment
    ) {
    }
}
