package com.ktb.answer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ktb.abuse.core.AbuseCheckContext;
import com.ktb.abuse.core.AbuseGuard;
import com.ktb.abuse.core.AbuseGuardResult;
import com.ktb.answer.domain.Answer;
import com.ktb.answer.domain.AnswerStatus;
import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.dto.AiFeedbackSummary;
import com.ktb.answer.dto.AnswerContentResult;
import com.ktb.answer.dto.AnswerDetailQuery;
import com.ktb.answer.dto.AnswerDetailResult;
import com.ktb.answer.dto.AnswerListCursor;
import com.ktb.answer.dto.AnswerSubmitCommand;
import com.ktb.answer.dto.AnswerSubmitResult;
import com.ktb.answer.dto.FeedbackResult;
import com.ktb.answer.dto.FeedbackStatus;
import com.ktb.answer.dto.ImmediateFeedbackResult;
import com.ktb.answer.dto.KeywordCheckResult;
import com.ktb.answer.dto.QuestionSummary;
import com.ktb.answer.dto.response.list.AnswerListResponse;
import com.ktb.answer.dto.response.list.AnswerSummary;
import com.ktb.answer.dto.response.list.FeedbackInfo;
import com.ktb.answer.dto.response.list.PaginationInfo;
import com.ktb.answer.dto.response.detail.AnswerQuestionInfo;
import com.ktb.answer.exception.AnswerAccessDeniedException;
import com.ktb.answer.exception.AnswerNotFoundException;
import com.ktb.answer.repository.AnswerRepository;
import com.ktb.answer.service.AnswerApplicationService;
import com.ktb.answer.service.AnswerDomainService;
import com.ktb.answer.service.ImmediateFeedbackService;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.service.UserAccountService;
import com.ktb.answer.exception.AnswerListInvalidInputException;
import com.ktb.file.exception.FileAlreadyDeletedException;
import com.ktb.file.exception.FileExtensionNotAllowedException;
import com.ktb.file.exception.FileNotFoundException;
import com.ktb.file.exception.FileSizeExceededException;
import com.ktb.file.exception.FileStorageMigrationException;
import com.ktb.hashtag.domain.AnswerHashtag;
import com.ktb.hashtag.domain.Hashtag;
import com.ktb.hashtag.exception.HashtagNotFoundException;
import com.ktb.hashtag.repository.AnswerHashtagRepository;
import com.ktb.hashtag.repository.HashtagRepository;
import com.ktb.metric.domain.AnswerMetric;
import com.ktb.metric.repository.AnswerMetricRepository;
import com.ktb.question.domain.Question;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import com.ktb.question.dto.QuestionDetailResponse;
import com.ktb.question.exception.QuestionDisabledException;
import com.ktb.question.exception.QuestionNotFoundException;
import com.ktb.question.service.QuestionService;

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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerApplicationServiceImpl implements AnswerApplicationService {

    private final AnswerRepository answerRepository;
    private final AnswerDomainService answerDomainService;
    private final ImmediateFeedbackService immediateFeedbackService;
    private final AnswerHashtagRepository answerHashtagRepository;
    private final HashtagRepository hashtagRepository;
    private final AnswerMetricRepository answerMetricRepository;
    private final AbuseGuard abuseGuard;

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

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
        log.debug("Retrieving answer list for accountId: {}", accountId);

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
    @Transactional
    public AnswerSubmitResult submit(Long accountId, AnswerSubmitCommand command, String clientIp)
            throws QuestionNotFoundException, QuestionDisabledException,
            FileSizeExceededException, FileExtensionNotAllowedException,
            FileNotFoundException, FileAlreadyDeletedException, FileStorageMigrationException {

        log.info("Submitting answer for questionId: {}, accountId: {}", command.questionId(), accountId);

        answerDomainService.validateAnswerContent(command.answerText());

        AbuseCheckContext abuseContext = AbuseCheckContext.of(
                accountId,
                command.questionId(),
                clientIp,
                command.answerText()
        );
        AbuseGuardResult abuseResult = abuseGuard.check(abuseContext);

        Answer answer = answerRepository.save(answerDomainService.createAnswer(
            accountId,
            command.questionId(),
            command.answerText(),
            command.answerType()
        ));

        ImmediateFeedbackResult immediateFeedback = immediateFeedbackService.evaluate(
                command.questionId(),
                answer.getContent()
        );

        saveAnswerHashtags(answer, immediateFeedback);

        if (!abuseResult.shouldProvideFeedback()) {
            answerDomainService.transitionStatus(answer, AnswerStatus.NOT_AVAILABLE);
            log.info("Answer saved without AI feedback - accountId: {}, reason: {}",
                    accountId, abuseResult.getReason());
            return AnswerSubmitResult.noAiFeedback(answer.getId(), immediateFeedback);
        }

        // TODO: MVP V2 AI 피드백 이벤트 발행
        return AnswerSubmitResult.processing(answer.getId(), immediateFeedback);
    }

    private void saveAnswerHashtags(Answer answer, ImmediateFeedbackResult feedback) {
        List<AnswerHashtag> answerHashtags = feedback.keywords().stream()
            .map(keywordResult -> {
                Hashtag hashtag = hashtagRepository.findById(keywordResult.keywordId())
                    .orElseThrow(() -> new HashtagNotFoundException(keywordResult.keywordId()));

                return AnswerHashtag.create(
                    answer,
                    hashtag,
                    keywordResult.included()
                );
            })
            .toList();

        answerHashtagRepository.saveAll(answerHashtags);

        log.debug("AnswerHashtags saved - answerId: {}, total: {}, included: {}",
                  answer.getId(),
                  answerHashtags.size(),
                  answerHashtags.stream().filter(ah -> ah.isIncluded()).count());
    }

    @Override
    @Transactional
    public AnswerSubmitResult submitWithSession(Long accountId, String sessionId, AnswerSubmitCommand command)
            throws QuestionNotFoundException {

        // TODO: MVP V2 구현 필요
        // 1. 세션 존재 및 소유권 검증
        // 2. 세션 상태 검증 (IN_PROGRESS만 허용)
        // 3. 중복 답변 검증 (동일 질문에 대한 답변)
        // 4. 파일 업로드 처리 (비디오 파일)
        // 5. Answer 생성 및 저장
        // 6. 즉각 피드백 생성
        // 7. 세션 depth 증가
        // 8. AI 피드백 이벤트 발행
        // 9. 다음 질문 생성 (필요 시)
        // 10. 결과 반환

        log.info("Submitting session answer for sessionId: {}, questionId: {}, accountId: {}",
                sessionId, command.questionId(), accountId);

        // TODO: ANSWER_SESSION 엔티티 구현 후 활성화
        throw new UnsupportedOperationException("Session-based answer submission not yet implemented");
    }

    @Override
    public AnswerDetailResult getDetail(Long accountId, Long answerId, AnswerDetailQuery query)
        throws AnswerNotFoundException, AnswerAccessDeniedException {

        log.debug("Retrieving answer detail for answerId: {}, accountId: {}", answerId, accountId);

        Answer answer = answerRepository.findByIdWithQuestion(answerId);
        if (answer == null) {
            throw new AnswerNotFoundException(answerId);
        }

        answerDomainService.validateOwnership(answer, accountId);

        AnswerContentResult answerContent = new AnswerContentResult(
            answer.getContent(),
            null,  // audioUrl (MVP V2: 파일 연동 시 구현)
            null,  // videoUrl (MVP V2: 파일 연동 시 구현)
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
        FeedbackResult feedback = buildFeedbackResult(answer);
        return new AiFeedbackSummary(
                feedback.status(),
                feedback.metrics(),
                feedback.comment()
        );
    }

    @Override
    public FeedbackResult getFeedback(Long accountId, Long answerId)
            throws AnswerNotFoundException, AnswerAccessDeniedException {

        log.debug("Retrieving feedback for answerId: {}, accountId: {}", answerId, accountId);

        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new AnswerNotFoundException(answerId));
        answerDomainService.validateOwnership(answer, accountId);

        return buildFeedbackResult(answer);
    }

    private FeedbackResult buildFeedbackResult(Answer answer) {
        FeedbackStatus status = FeedbackStatus.from(answer.getStatus());

        if (status == FeedbackStatus.PROCESSING) {
            return new FeedbackResult(status, 30, null, null, null);
        }

        if (status != FeedbackStatus.COMPLETED) {
            return new FeedbackResult(status, null, null, null, null);
        }

        List<AnswerMetric> metrics = answerMetricRepository.findByAnswerIdWithMetric(answer.getId());

        Map<String, Integer> metricsMap = metrics.stream()
                .collect(Collectors.toMap(
                        AnswerMetric::getMetricName,
                        AnswerMetric::getScore
                ));

        return new FeedbackResult(
                status,
                null,
                metricsMap,
                answer.getAiFeedback(),
                answer.getUpdatedAt() != null
                        ? answer.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
                        : null
        );
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
        if (questionType != QuestionType.CS) {
            throw new AnswerListInvalidInputException("questionType is only supported for [CS, SYSTEM_DESIGN, PORTFOLIO]");
        }
    }

    private record LocalDateRange(LocalDate start, LocalDate end) {
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
}
