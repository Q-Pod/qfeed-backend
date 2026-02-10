package com.ktb.answer.service;

import com.ktb.answer.dto.ImmediateFeedbackResult;
import com.ktb.answer.dto.KeywordCheckResult;
import com.ktb.answer.service.impl.ImmediateFeedbackServiceImpl;
import com.ktb.fixture.QuestionHashtagFixture;
import com.ktb.hashtag.domain.QuestionHashtag;
import com.ktb.hashtag.repository.QuestionHashtagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ImmediateFeedbackService 단위 테스트")
class ImmediateFeedbackServiceTest {

    @Mock
    private QuestionHashtagRepository questionHashtagRepository;

    @InjectMocks
    private ImmediateFeedbackServiceImpl immediateFeedbackService;

    private static final Long QUESTION_ID = 100L;

    @Nested
    @DisplayName("evaluate() 테스트")
    class EvaluateTest {

        @Test
        @DisplayName("답변에 키워드가 포함된 경우 included=true 반환")
        void evaluate_WithKeywordIncluded_ShouldReturnTrue() {
            // Given
            String answerText = "데이터베이스 인덱스를 사용하면 조회 성능이 향상됩니다.";
            List<QuestionHashtag> questionHashtags = QuestionHashtagFixture.createMockQuestionHashtags("인덱스", "성능");

            when(questionHashtagRepository.findKeywordNamesByQuestionId(QUESTION_ID))
                    .thenReturn(questionHashtags);

            // When
            ImmediateFeedbackResult result = immediateFeedbackService.evaluate(QUESTION_ID, answerText);

            // Then
            assertThat(result.keywords()).hasSize(2);
            assertThat(result.keywords())
                    .allMatch(KeywordCheckResult::included);
        }

        @Test
        @DisplayName("답변에 키워드가 포함되지 않은 경우 included=false 반환")
        void evaluate_WithKeywordNotIncluded_ShouldReturnFalse() {
            // Given
            String answerText = "테스트 답변입니다.";
            List<QuestionHashtag> questionHashtags = QuestionHashtagFixture.createMockQuestionHashtags("인덱스", "성능");

            when(questionHashtagRepository.findKeywordNamesByQuestionId(QUESTION_ID))
                    .thenReturn(questionHashtags);

            // When
            ImmediateFeedbackResult result = immediateFeedbackService.evaluate(QUESTION_ID, answerText);

            // Then
            assertThat(result.keywords()).hasSize(2);
            assertThat(result.keywords())
                    .noneMatch(KeywordCheckResult::included);
        }

        @Test
        @DisplayName("질문에 키워드가 없는 경우 빈 목록 반환")
        void evaluate_WithNoKeywords_ShouldReturnEmptyList() {
            // Given
            String answerText = "테스트 답변입니다.";

            when(questionHashtagRepository.findKeywordNamesByQuestionId(QUESTION_ID))
                    .thenReturn(Collections.emptyList());

            // When
            ImmediateFeedbackResult result = immediateFeedbackService.evaluate(QUESTION_ID, answerText);

            // Then
            assertThat(result.keywords()).isEmpty();
        }
    }

    @Nested
    @DisplayName("checkKeywords() 테스트")
    class CheckKeywordsTest {

        @Test
        @DisplayName("대소문자 무시하고 키워드 매칭")
        void checkKeywords_WithDifferentCase_ShouldMatch() {
            // Given
            String answerText = "INDEX를 사용하면 성능이 향상됩니다.";
            List<QuestionHashtag> questionHashtags = QuestionHashtagFixture.createMockQuestionHashtags("index");

            // When
            List<KeywordCheckResult> results = immediateFeedbackService.checkKeywords(answerText, questionHashtags);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().included()).isTrue();
        }

        @Test
        @DisplayName("정확히 일치하는 키워드 매칭")
        void checkKeywords_WithExactMatch_ShouldMatch() {
            // Given
            String answerText = "정규화를 통해 데이터 중복을 제거합니다.";
            List<QuestionHashtag> questionHashtags = QuestionHashtagFixture.createMockQuestionHashtags("정규화");

            // When
            List<KeywordCheckResult> results = immediateFeedbackService.checkKeywords(answerText, questionHashtags);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().included()).isTrue();
            assertThat(results.getFirst().keyword()).isEqualTo("정규화");
        }

        @Test
        @DisplayName("부분 문자열로 키워드 매칭")
        void checkKeywords_WithPartialMatch_ShouldMatch() {
            // Given
            String answerText = "비정규화를 사용합니다.";
            List<QuestionHashtag> questionHashtags = QuestionHashtagFixture.createMockQuestionHashtags("정규화");

            // When
            List<KeywordCheckResult> results = immediateFeedbackService.checkKeywords(answerText, questionHashtags);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.getFirst().included()).isTrue();
        }

        @Test
        @DisplayName("다중 키워드 중 일부만 포함된 경우")
        void checkKeywords_WithMultipleKeywords_ShouldMatchSome() {
            // Given
            String answerText = "인덱스를 사용합니다.";
            List<QuestionHashtag> questionHashtags = QuestionHashtagFixture.createMockQuestionHashtags(
                    "인덱스", "정규화", "트랜잭션"
            );

            // When
            List<KeywordCheckResult> results = immediateFeedbackService.checkKeywords(answerText, questionHashtags);

            // Then
            assertThat(results).hasSize(3);

            long includedCount = results.stream()
                    .filter(KeywordCheckResult::included)
                    .count();
            assertThat(includedCount).isEqualTo(1);
        }

        @Test
        @DisplayName("빈 키워드 목록 처리")
        void checkKeywords_WithEmptyKeywords_ShouldReturnEmptyList() {
            // Given
            String answerText = "테스트 답변입니다.";
            List<QuestionHashtag> emptyHashtags = Collections.emptyList();

            // When
            List<KeywordCheckResult> results = immediateFeedbackService.checkKeywords(answerText, emptyHashtags);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractKeywords() 테스트")
    class ExtractKeywordsTest {

        @Test
        @DisplayName("질문 ID로 키워드 추출 성공")
        void extractKeywords_WithValidQuestionId_ShouldReturnKeywords() {
            // Given
            Long testQuestionId = 200L;
            List<QuestionHashtag> expectedHashtags = QuestionHashtagFixture.createMockQuestionHashtags("키워드1", "키워드2");

            when(questionHashtagRepository.findKeywordNamesByQuestionId(testQuestionId))
                    .thenReturn(expectedHashtags);

            // When
            List<QuestionHashtag> results = immediateFeedbackService.extractKeywords(testQuestionId);

            // Then
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("키워드가 없는 질문에서 빈 목록 반환")
        void extractKeywords_WithNoKeywords_ShouldReturnEmptyList() {
            // Given
            Long testQuestionId = 300L;
            when(questionHashtagRepository.findKeywordNamesByQuestionId(testQuestionId))
                    .thenReturn(Collections.emptyList());

            // When
            List<QuestionHashtag> results = immediateFeedbackService.extractKeywords(testQuestionId);

            // Then
            assertThat(results).isEmpty();
        }
    }
}
