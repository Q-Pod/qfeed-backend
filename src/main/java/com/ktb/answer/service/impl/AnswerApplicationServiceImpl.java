package com.ktb.answer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.answer.domain.Answer;
import com.ktb.answer.domain.AnswerStatus;
import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.dto.AnswerDetailQuery;
import com.ktb.answer.dto.AnswerDetailResult;
import com.ktb.answer.dto.AnswerListCursor;
import com.ktb.answer.dto.AnswerSubmitCommand;
import com.ktb.answer.dto.AnswerSubmitResult;
import com.ktb.answer.dto.FeedbackResult;
import com.ktb.answer.dto.ImmediateFeedbackResult;
import com.ktb.answer.dto.response.AnswerListResponse;
import com.ktb.answer.exception.AnswerAccessDeniedException;
import com.ktb.answer.exception.AnswerNotFoundException;
import com.ktb.answer.repository.AnswerRepository;
import com.ktb.answer.service.AnswerApplicationService;
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
    private final QuestionService questionService;
    private final UserAccountService userAccountService;
    private final ImmediateFeedbackService immediateFeedbackService;
    private final AnswerHashtagRepository answerHashtagRepository;
    private final HashtagRepository hashtagRepository;
    private final ObjectMapper objectMapper;

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
    public AnswerSubmitResult submit(Long accountId, AnswerSubmitCommand command)
            throws QuestionNotFoundException, QuestionDisabledException,
            FileSizeExceededException, FileExtensionNotAllowedException,
            FileNotFoundException, FileAlreadyDeletedException, FileStorageMigrationException {

        log.info("Submitting answer for questionId: {}, accountId: {}", command.questionId(), accountId);

        QuestionDetailResponse question = questionService.getQuestionDetail(command.questionId());
        validateQuestionEnabled(question);

        UserAccount account = userAccountService.findById(accountId);

        Answer answer = Answer.create(
                Question.createWithQuestionId(question.questionId()),
                account,
                command.answerText(),
                command.answerType()
        );

        Answer savedAnswer = answerRepository.save(answer);

        ImmediateFeedbackResult immediateFeedback = immediateFeedbackService.evaluate(
                question.questionId(),
                savedAnswer.getContent()
        );

        saveAnswerHashtags(savedAnswer, immediateFeedback);

        // TODO: MVP V2 AI 피드백 이벤트 발행
        return AnswerSubmitResult.processing(savedAnswer.getId(), immediateFeedback);
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

        // TODO: MVP V2 구현 필요
        // 1. Answer 조회
        // 2. 소유권 검증
        // 3. expand 파라미터에 따라 추가 데이터 조회
        //    - expand=question: Question 정보 포함
        //    - expand=feedback: ANSWER_METRIC, AI 피드백 포함
        //    - expand=immediate_feedback: 즉각 피드백 포함
        // 4. DTO로 변환하여 반환

        log.debug("Retrieving answer detail for answerId: {}, accountId: {}", answerId, accountId);

        Answer answer = findAnswerById(answerId);
        validateOwnership(answer, accountId);

        // TODO: expand 처리 및 DTO 변환
        return null;
    }

    @Override
    public FeedbackResult getFeedback(Long accountId, Long answerId)
            throws AnswerNotFoundException, AnswerAccessDeniedException {

        // TODO: 구현 필요
        // 1. Answer 조회
        // 2. 소유권 검증
        // 3. Answer 상태에 따라 응답 분기
        //    - AI_FEEDBACK_PROCESSING: 202 응답 (retry_after 포함)
        //    - COMPLETED: 200 응답 (레이더 차트, AI 피드백 포함)
        //    - FAILED: 200 응답 (실패 사유 포함)
        // 4. ANSWER_METRIC 조회 (레이더 차트 데이터)
        // 5. DTO로 변환하여 반환

        log.debug("Retrieving feedback for answerId: {}, accountId: {}", answerId, accountId);

        Answer answer = findAnswerById(answerId);
        validateOwnership(answer, accountId);

        // TODO: 상태별 응답 처리 및 DTO 변환
        return null;
    }

    private void validateQuestionEnabled(QuestionDetailResponse question) {
        if (!question.useYn()) {
            throw new QuestionDisabledException(question.questionId());
        }
    }

    private Answer findAnswerById(Long answerId) {
        Answer answer = answerRepository.findByIdWithQuestion(answerId);
        if (answer == null) {
            throw new AnswerNotFoundException(answerId);
        }
        return answer;
    }

    private void validateOwnership(Answer answer, Long accountId) {
        if (!answer.isOwnedBy(accountId)) {
            throw new AnswerAccessDeniedException(answer.getId(), accountId);
        }
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
            AnswerListCursor payload = objectMapper.readValue(decoded, AnswerListCursor.class);
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
            byte[] json = objectMapper.writeValueAsBytes(cursor);
            return Base64.getEncoder().encodeToString(json);
        } catch (JsonProcessingException e) {
            throw new AnswerListInvalidInputException("failed to encode cursor", e);
        }
    }

    private AnswerListResponse toAnswerListResponse(Slice<Answer> answers, int limit) {
        List<AnswerListResponse.AnswerSummary> records = answers.getContent().stream()
                .map(answer -> new AnswerListResponse.AnswerSummary(
                        answer.getId(),
                        answer.getType().name(),
                        answer.getCreatedAt() == null ? null : answer.getCreatedAt().toString(),
                        new AnswerListResponse.QuestionInfo(
                                answer.getQuestion().getId(),
                                answer.getQuestion().getContent(),
                                answer.getQuestion().getCategory().name()
                        ),
                        toFeedbackInfo(answer)
                ))
                .toList();

        String nextCursor = null;
        if (answers.hasNext() && !answers.getContent().isEmpty()) {
            Answer last = answers.getContent().get(answers.getContent().size() - 1);
            nextCursor = encodeCursor(new AnswerListCursor(last.getCreatedAt(), last.getId()));
        }

        AnswerListResponse.PaginationInfo pagination = new AnswerListResponse.PaginationInfo(
                limit,
                answers.hasNext(),
                nextCursor
        );

        return new AnswerListResponse(records, pagination);
    }

    private AnswerListResponse.FeedbackInfo toFeedbackInfo(Answer answer) {
        AnswerStatus status = answer.getStatus();
        String feedbackStatus = switch (status) {
            case AI_FEEDBACK_PROCESSING -> "PROCESSING";
            case COMPLETED -> "COMPLETED";
            case FAILED -> "FAILED";
            case FAILED_RETRYABLE -> "FAILED_RETRYABLE";
            default -> "NOT_AVAILABLE";
        };
        boolean available = status == AnswerStatus.COMPLETED;
        return new AnswerListResponse.FeedbackInfo(available, feedbackStatus);
    }
}
