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
import com.ktb.question.dto.QuestionCreateRequest;
import com.ktb.question.dto.QuestionDetailResponse;
import com.ktb.question.dto.QuestionKeywordCheckResponse;
import com.ktb.question.dto.QuestionKeywordListResponse;
import com.ktb.question.dto.QuestionListResponse;
import com.ktb.question.dto.QuestionSearchResponse;
import com.ktb.question.dto.QuestionSummaryResponse;
import com.ktb.question.dto.QuestionUpdateRequest;
import com.ktb.question.exception.QuestionNotFoundException;
import com.ktb.question.exception.SearchKeywordTooShortException;
import com.ktb.question.repository.QuestionRepository;
import com.ktb.question.service.QuestionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionServiceImpl implements QuestionService {

    private static final int MIN_SEARCH_KEYWORD_LENGTH = 2;

    private final QuestionRepository questionRepository;
    private final QuestionHashtagRepository questionHashtagRepository;
    private final HashtagRepository hashtagRepository;

    @Override
    public QuestionListResponse getQuestions(QuestionCategory category, QuestionType type, Long cursor, int size) {
        PageRequest pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "id"));
        Slice<Question> questions = questionRepository.findActiveByFilters(type, category, cursor, pageable);
        Map<Long, List<String>> keywordMap = loadKeywordsByQuestionIds(questions.getContent());

        return new QuestionListResponse(
                toSummaryResponses(questions.getContent(), keywordMap),
                toPaginationResponse(questions)
        );
    }

    @Override
    public QuestionDetailResponse getQuestionDetail(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        return toDetailResponse(question);
    }

    @Override
    public QuestionSearchResponse search(String keyword, QuestionCategory category, QuestionType type, Long cursor, int size) {
        validateKeyword(keyword);

        PageRequest pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "id"));
        Slice<Question> questions = questionRepository.searchActiveByKeyword(keyword, type, category, cursor, pageable);
        Map<Long, List<String>> keywordMap = loadKeywordsByQuestionIds(questions.getContent());

        return new QuestionSearchResponse(
                toSummaryResponses(questions.getContent(), keywordMap),
                toPaginationResponse(questions)
        );
    }

    @Override
    public QuestionDetailResponse getDailyRecommendation() {
        Long questionId = questionRepository.findRandomActiveId()
                .orElseThrow(() -> new QuestionNotFoundException(0L));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        return toDetailResponse(question);
    }

    @Override
    @Transactional
    public QuestionDetailResponse createQuestion(QuestionCreateRequest request) {
        Question question = Question.create(request.content(), request.type(), request.category());
        Question saved = questionRepository.save(question);
        attachKeywords(saved, request.keywords());

        return toDetailResponse(saved);
    }

    @Override
    @Transactional
    public QuestionDetailResponse updateQuestion(Long questionId, QuestionUpdateRequest request) {
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

        return toDetailResponse(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        if (question.isUseYn()) {
            question.delete();
        }
    }

    @Override
    public QuestionKeywordListResponse getQuestionKeywords(Long questionId) {
        validateQuestionExists(questionId);
        List<QuestionHashtag> tags = questionHashtagRepository.findKeywordNamesByQuestionId(questionId);

        List<String> keywords = tags.stream()
            .map(questionHashtag -> questionHashtag.getHashtag().getName())
            .toList();

        return new QuestionKeywordListResponse(keywords);
    }

    @Override
    public QuestionKeywordCheckResponse checkQuestionKeywords(Long questionId, List<String> keywords) {
        validateQuestionExists(questionId);
        List<KeywordMatchResponse> results = keywords.stream()
                .map(keyword -> new KeywordMatchResponse(
                        keyword,
                        questionHashtagRepository
                                .existsByQuestion_IdAndHashtag_NameIgnoreCase(questionId, keyword)
                ))
                .toList();
        return new QuestionKeywordCheckResponse(results);
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
    }

    private void replaceKeywords(Question question, List<String> keywords) {
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
