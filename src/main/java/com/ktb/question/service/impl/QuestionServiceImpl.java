package com.ktb.question.service.impl;

import com.ktb.hashtag.domain.Hashtag;
import com.ktb.hashtag.domain.QuestionHashtag;
import com.ktb.hashtag.repository.HashtagRepository;
import com.ktb.hashtag.repository.QuestionKeywordRow;
import com.ktb.hashtag.repository.QuestionHashtagRepository;
import com.ktb.question.domain.Question;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import com.ktb.question.dto.KeywordMatchResponse;
import com.ktb.question.dto.PaginationResponse;
import com.ktb.question.dto.QuestionCategoryListResponse;
import com.ktb.question.dto.QuestionCreateRequest;
import com.ktb.question.dto.QuestionDetailResponse;
import com.ktb.question.dto.QuestionKeywordCheckResponse;
import com.ktb.question.dto.QuestionKeywordListResponse;
import com.ktb.question.dto.QuestionListResponse;
import com.ktb.question.dto.QuestionSearchResponse;
import com.ktb.question.dto.QuestionSummaryResponse;
import com.ktb.question.dto.QuestionTypeListResponse;
import com.ktb.question.dto.QuestionUpdateRequest;
import com.ktb.question.exception.QuestionNotFoundException;
import com.ktb.question.exception.SearchKeywordTooShortException;
import com.ktb.question.repository.QuestionRepository;
import com.ktb.question.service.QuestionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private static final int MIN_SEARCH_KEYWORD_LENGTH = 2;
    private static final Set<QuestionType> EXPOSED_TYPES = EnumSet.of(
            QuestionType.CS,
            QuestionType.SYSTEM_DESIGN
    );

    private final QuestionRepository questionRepository;
    private final QuestionHashtagRepository questionHashtagRepository;
    private final HashtagRepository hashtagRepository;

    @Override
    public QuestionListResponse getQuestions(QuestionCategory category, QuestionType type, Long cursor, int size) {
        log.debug("getQuestions - type: {}, category: {}, cursor: {}, size: {}",
                type, category, cursor, size);
        PageRequest pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "id"));
        Slice<Question> questions = questionRepository.findActiveByFilters(type, category, cursor, pageable);
        Map<Long, List<String>> keywordMap = loadKeywordsByQuestionIds(questions.getContent());

        log.debug("getQuestions success - count: {}, hasNext: {}",
                questions.getContent().size(), questions.hasNext());

        return new QuestionListResponse(
                toSummaryResponses(questions.getContent(), keywordMap),
                toPaginationResponse(questions)
        );
    }

    @Override
    public QuestionDetailResponse getQuestionDetail(Long questionId) {
        log.debug("getQuestionDetail - questionId: {}", questionId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        log.debug("getQuestionDetail success - questionId: {}, type: {}, category: {}",
                question.getId(), question.getType(), question.getCategory());
        return toDetailResponse(question);
    }

    @Override
    public QuestionSearchResponse search(String keyword, QuestionCategory category, QuestionType type, Long cursor, int size) {
        log.debug("search - keyword: {}, type: {}, category: {}, cursor: {}, size: {}",
                keyword, type, category, cursor, size);
        validateKeyword(keyword);

        PageRequest pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "id"));
        Slice<Question> questions = questionRepository.searchActiveByKeyword(keyword, type, category, cursor, pageable);
        Map<Long, List<String>> keywordMap = loadKeywordsByQuestionIds(questions.getContent());

        log.debug("search success - count: {}, hasNext: {}",
                questions.getContent().size(), questions.hasNext());

        return new QuestionSearchResponse(
                toSummaryResponses(questions.getContent(), keywordMap),
                toPaginationResponse(questions)
        );
    }

    @Override
    public QuestionDetailResponse getDailyRecommendation() {
        log.debug("getDailyRecommendation");
        Long questionId = questionRepository.findRandomActiveId()
                .orElseThrow(() -> new QuestionNotFoundException(0L));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        log.info("getDailyRecommendation success - questionId: {}, type: {}, category: {}",
                question.getId(), question.getType(), question.getCategory());
        return toDetailResponse(question);
    }

    @Override
    @Transactional
    public QuestionDetailResponse createQuestion(QuestionCreateRequest request) {
        int keywordCount = request.keywords() == null ? 0 : request.keywords().size();
        log.info("createQuestion - type: {}, category: {}, keywordCount: {}",
                request.type(), request.category(), keywordCount);
        Question question = Question.create(request.content(), request.type(), request.category());
        Question saved = questionRepository.save(question);
        attachKeywords(saved, request.keywords());

        log.info("createQuestion success - questionId: {}", saved.getId());
        return toDetailResponse(saved);
    }

    @Override
    @Transactional
    public QuestionDetailResponse updateQuestion(Long questionId, QuestionUpdateRequest request) {
        int keywordCount = request.keywords() == null ? 0 : request.keywords().size();
        log.info("updateQuestion - questionId: {}, hasContent: {}, type: {}, category: {}, useYn: {}, keywordCount: {}",
                questionId, request.content() != null, request.type(), request.category(), request.useYn(), keywordCount);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        if (request.content() != null) {
            question.updateContent(request.content());
        }
        if (request.type() != null) {
            question.updateType(request.type());
        }
        if (request.category() != null) {
            question.updateCategory(request.category());
        }
        if (request.useYn() != null) {
            if (request.useYn()) {
                question.activate();
            } else {
                question.delete();
            }
        }
        if (request.keywords() != null) {
            replaceKeywords(question, request.keywords());
        }

        log.info("updateQuestion success - questionId: {}", questionId);
        return toDetailResponse(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        log.info("deleteQuestion - questionId: {}", questionId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        if (question.isUseYn()) {
            question.delete();
            log.info("deleteQuestion success - questionId: {}, deleted: true", questionId);
            return;
        }
        log.info("deleteQuestion skipped - questionId: {}, reason: already inactive", questionId);
    }

    @Override
    public QuestionKeywordListResponse getQuestionKeywords(Long questionId) {
        log.debug("getQuestionKeywords - questionId: {}", questionId);
        validateQuestionExists(questionId);
        List<QuestionHashtag> tags = questionHashtagRepository.findKeywordNamesByQuestionId(questionId);

        List<String> keywords = tags.stream()
            .map(questionHashtag -> questionHashtag.getHashtag().getName())
            .toList();

        log.debug("getQuestionKeywords success - questionId: {}, count: {}", questionId, keywords.size());
        return new QuestionKeywordListResponse(keywords);
    }

    @Override
    public QuestionKeywordCheckResponse checkQuestionKeywords(Long questionId, List<String> keywords) {
        int keywordCount = keywords == null ? 0 : keywords.size();
        log.debug("checkQuestionKeywords - questionId: {}, keywordCount: {}", questionId, keywordCount);
        validateQuestionExists(questionId);
        List<KeywordMatchResponse> results = keywords.stream()
                .map(keyword -> new KeywordMatchResponse(
                        keyword,
                        questionHashtagRepository
                                .existsByQuestion_IdAndHashtag_NameIgnoreCase(questionId, keyword)
                ))
                .toList();
        long includedCount = results.stream()
                .filter(KeywordMatchResponse::included)
                .count();
        log.debug("checkQuestionKeywords success - questionId: {}, includedCount: {}",
                questionId, includedCount);
        return new QuestionKeywordCheckResponse(results);
    }

    @Override
    public QuestionCategoryListResponse getQuestionCategories() {
        log.debug("getQuestionCategories");
        Map<String, Map<String, String>> categories = EXPOSED_TYPES.stream()
                .collect(Collectors.toMap(
                        QuestionType::name,
                        type -> Arrays.stream(QuestionCategory.values())
                                .filter(category -> category.supports(type))
                                .collect(Collectors.toMap(
                                        QuestionCategory::name,
                                        QuestionCategory::getCategory,
                                        (existing, ignored) -> existing,
                                        LinkedHashMap::new
                                )),
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));
        log.debug("getQuestionCategories success - typeGroups: {}", categories.size());
        return new QuestionCategoryListResponse(categories);
    }

    @Override
    public QuestionTypeListResponse getQuestionTypes() {
        log.debug("getQuestionTypes");
        Map<String, String> types = Arrays.stream(QuestionType.values())
                .filter(EXPOSED_TYPES::contains)
                .collect(Collectors.toMap(
                        QuestionType::name,
                        QuestionType::getType,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));
        log.debug("getQuestionTypes success - count: {}", types.size());
        return new QuestionTypeListResponse(types);
    }

    private void validateKeyword(String keyword) {
        if (keyword == null || keyword.trim().length() < MIN_SEARCH_KEYWORD_LENGTH) {
            throw new SearchKeywordTooShortException(MIN_SEARCH_KEYWORD_LENGTH);
        }
    }

    private List<QuestionSummaryResponse> toSummaryResponses(
            List<Question> questions,
            Map<Long, List<String>> keywordMap
    ) {
        return questions.stream()
                .map(question -> new QuestionSummaryResponse(
                        question.getId(),
                        question.getContent(),
                        question.getType(),
                        question.getCategory(),
                        keywordMap.getOrDefault(question.getId(), List.of())
                ))
                .toList();
    }

    private QuestionDetailResponse toDetailResponse(Question question) {
        List<QuestionHashtag> tags = questionHashtagRepository.findKeywordNamesByQuestionId(question.getId());

        List<String> keywords = tags.stream()
            .map(questionHashtag -> questionHashtag.getHashtag().getName())
            .toList();
        return new QuestionDetailResponse(
                question.getId(),
                question.getContent(),
                question.getType(),
                question.getCategory(),
                keywords,
                question.isUseYn(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                question.getDeletedAt()
        );
    }

    private PaginationResponse toPaginationResponse(Slice<Question> slice) {
        Long nextCursor = null;
        if (!slice.getContent().isEmpty()) {
            Question last = slice.getContent().get(slice.getContent().size() - 1);
            nextCursor = slice.hasNext() ? last.getId() : null;
        }
        return new PaginationResponse(nextCursor, slice.hasNext(), slice.getSize());
    }

    private void validateQuestionExists(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new QuestionNotFoundException(questionId);
        }
    }

    private void attachKeywords(Question question, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return;
        }

        List<String> normalized = keywords.stream()
                .map(keyword -> keyword.trim().toLowerCase())
                .distinct()
                .toList();

        List<Hashtag> existing = hashtagRepository.findByNameIn(normalized);
        Map<String, Hashtag> existingByName = existing.stream()
                .collect(Collectors.toMap(Hashtag::getName, Function.identity()));

        List<Hashtag> toCreate = new ArrayList<>();
        for (String name : normalized) {
            if (!existingByName.containsKey(name)) {
                toCreate.add(Hashtag.create(name));
            }
        }

        if (!toCreate.isEmpty()) {
            List<Hashtag> created = hashtagRepository.saveAll(toCreate);
            for (Hashtag hashtag : created) {
                existingByName.put(hashtag.getName(), hashtag);
            }
        }

        List<QuestionHashtag> mappings = new ArrayList<>();
        for (String name : normalized) {
            Hashtag hashtag = existingByName.get(name);
            if (hashtag != null) {
                mappings.add(QuestionHashtag.create(question, hashtag));
            }
        }

        if (!mappings.isEmpty()) {
            questionHashtagRepository.saveAll(mappings);
        }

        log.debug("attachKeywords success - questionId: {}, requested: {}, mapped: {}",
                question.getId(), keywords.size(), mappings.size());
    }

    private void replaceKeywords(Question question, List<String> keywords) {
        log.debug("replaceKeywords - questionId: {}, keywordCount: {}",
                question.getId(), keywords == null ? 0 : keywords.size());
        questionHashtagRepository.deleteByQuestion_Id(question.getId());
        if (keywords == null || keywords.isEmpty()) {
            return;
        }
        attachKeywords(question, keywords);
    }

    private Map<Long, List<String>> loadKeywordsByQuestionIds(List<Question> questions) {
        if (questions.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = questions.stream()
                .map(Question::getId)
                .toList();
        List<QuestionKeywordRow> rows = questionHashtagRepository.findKeywordRowsByQuestionIdIn(ids);
        return rows.stream()
                .collect(Collectors.groupingBy(
                        QuestionKeywordRow::getQuestionId,
                        Collectors.mapping(QuestionKeywordRow::getKeyword, Collectors.toList())
                ));
    }
}
