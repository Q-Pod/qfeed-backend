package com.ktb.question.service;

import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import com.ktb.question.dto.*;

public interface QuestionService {

    QuestionListResponse getQuestions(QuestionCategory category, QuestionType type, Long cursor, int size);

    QuestionDetailResponse getQuestionDetail(Long questionId);

    QuestionSearchResponse search(String keyword, QuestionCategory category, QuestionType type, Long cursor, int size);

    QuestionDetailResponse createQuestion(QuestionCreateRequest request);

    QuestionDetailResponse updateQuestion(Long questionId, QuestionUpdateRequest request);

    void deleteQuestion(Long questionId);

    QuestionDetailResponse getDailyRecommendation();

    QuestionKeywordListResponse getQuestionKeywords(Long questionId);

    QuestionKeywordCheckResponse checkQuestionKeywords(Long questionId, java.util.List<String> keywords);

    QuestionCategoryListResponse getQuestionCategories();

    QuestionTypeListResponse getQuestionTypes();
}
