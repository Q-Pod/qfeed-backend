package com.ktb.question.service;

import com.ktb.hashtag.domain.Hashtag;
import com.ktb.hashtag.domain.QuestionHashtag;
import com.ktb.hashtag.repository.HashtagRepository;
import com.ktb.hashtag.repository.QuestionHashtagRepository;
import com.ktb.hashtag.repository.QuestionKeywordRow;
import com.ktb.question.domain.Question;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import com.ktb.question.dto.QuestionCategoryListResponse;
import com.ktb.question.dto.QuestionDetailResponse;
import com.ktb.question.dto.QuestionKeywordCheckResponse;
import com.ktb.question.dto.QuestionListResponse;
import com.ktb.question.dto.QuestionUpdateRequest;
import com.ktb.question.dto.QuestionTypeListResponse;
import com.ktb.question.exception.QuestionNotFoundException;
import com.ktb.question.exception.SearchKeywordTooShortException;
import com.ktb.question.repository.QuestionRepository;
import com.ktb.question.service.impl.QuestionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionService 단위 테스트")
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionHashtagRepository questionHashtagRepository;

    @Mock
    private HashtagRepository hashtagRepository;

    @InjectMocks
    private QuestionServiceImpl questionService;

    @Nested
    @DisplayName("질문 타입/카테고리 메타 조회 테스트")
    class TypeCategoryMetaTest {

        @Test
        @DisplayName("getQuestionCategories는 CS, SYSTEM_DESIGN 그룹만 반환")
        void getQuestionCategories_ShouldReturnOnlyExposedTypeGroups() {
            // When
            QuestionCategoryListResponse result = questionService.getQuestionCategories();

            // Then
            Map<String, Map<String, String>> categories = result.categories();
            assertThat(categories).containsOnlyKeys("CS", "SYSTEM_DESIGN");
            assertThat(categories.get("CS"))
                    .containsKeys("OS", "NETWORK", "DB", "COMPUTER_ARCHITECTURE", "DATA_STRUCTURE_ALGORITHM");
            assertThat(categories.get("SYSTEM_DESIGN"))
                    .containsKeys("SOCIAL", "NOTIFICATION", "REALTIME", "SEARCH", "MEDIA", "STORAGE", "PLATFORM", "TRANSACTION");
            assertThat(categories.get("SYSTEM_DESIGN")).doesNotContainKey("PORTFOLIO");
        }

        @Test
        @DisplayName("getQuestionTypes는 CS, SYSTEM_DESIGN만 반환")
        void getQuestionTypes_ShouldExcludePortfolio() {
            // When
            QuestionTypeListResponse result = questionService.getQuestionTypes();

            // Then
            assertThat(result.types()).containsOnlyKeys("CS", "SYSTEM_DESIGN");
            assertThat(result.types()).doesNotContainKey("PORTFOLIO");
        }
    }

    @Nested
    @DisplayName("질문 조회 테스트")
    class GetQuestionsTest {

        @Test
        @DisplayName("질문 목록 조회 시 키워드 매핑과 pagination 정보 반환")
        void getQuestions_ShouldReturnMappedKeywordsAndPagination() {
            // Given
            Question first = mockQuestion(200L, "실시간 처리 질문", QuestionType.SYSTEM_DESIGN, QuestionCategory.REALTIME);
            Question second = mockQuestion(150L, "운영체제 질문", QuestionType.CS, QuestionCategory.OS);
            Slice<Question> slice = new SliceImpl<>(List.of(first, second), PageRequest.of(0, 2), true);

            when(questionRepository.findActiveByFilters(
                    eq(QuestionType.SYSTEM_DESIGN), eq(QuestionCategory.REALTIME), isNull(), any()))
                    .thenReturn(slice);
            when(questionHashtagRepository.findKeywordRowsByQuestionIdIn(List.of(200L, 150L)))
                    .thenReturn(List.of(
                            keywordRow(200L, "websocket"),
                            keywordRow(150L, "thread"),
                            keywordRow(150L, "process")
                    ));

            // When
            QuestionListResponse result = questionService.getQuestions(
                    QuestionCategory.REALTIME,
                    QuestionType.SYSTEM_DESIGN,
                    null,
                    2
            );

            // Then
            assertThat(result.questions()).hasSize(2);
            assertThat(result.questions().get(0).questionId()).isEqualTo(200L);
            assertThat(result.questions().get(0).keywords()).containsExactly("websocket");
            assertThat(result.questions().get(1).questionId()).isEqualTo(150L);
            assertThat(result.questions().get(1).keywords()).containsExactly("thread", "process");
            assertThat(result.pagination().hasNext()).isTrue();
            assertThat(result.pagination().nextCursor()).isEqualTo(150L);
            assertThat(result.pagination().size()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("질문 검색 테스트")
    class SearchTest {

        @Test
        @DisplayName("검색어가 2자 미만이면 예외 발생")
        void search_WithTooShortKeyword_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> questionService.search("a", null, null, null, 10))
                    .isInstanceOf(SearchKeywordTooShortException.class);
            verifyNoInteractions(questionRepository);
        }
    }

    @Nested
    @DisplayName("질문 상세/키워드 조회 테스트")
    class DetailAndKeywordTest {

        @Test
        @DisplayName("질문 상세 조회 성공")
        void getQuestionDetail_ShouldReturnDetailWithKeywords() {
            // Given
            Long questionId = 10L;
            Question question = mockQuestion(questionId, "질문 본문", QuestionType.CS, QuestionCategory.DB);
            QuestionHashtag redisHashtag = questionHashtag("redis");
            QuestionHashtag indexHashtag = questionHashtag("index");
            when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
            when(questionHashtagRepository.findKeywordNamesByQuestionId(questionId))
                    .thenReturn(List.of(redisHashtag, indexHashtag));

            // When
            QuestionDetailResponse result = questionService.getQuestionDetail(questionId);

            // Then
            assertThat(result.questionId()).isEqualTo(questionId);
            assertThat(result.type()).isEqualTo(QuestionType.CS);
            assertThat(result.category()).isEqualTo(QuestionCategory.DB);
            assertThat(result.keywords()).containsExactly("redis", "index");
        }

        @Test
        @DisplayName("존재하지 않는 질문 상세 조회 시 QuestionNotFoundException")
        void getQuestionDetail_WithUnknownId_ShouldThrowException() {
            // Given
            when(questionRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> questionService.getQuestionDetail(999L))
                    .isInstanceOf(QuestionNotFoundException.class);
        }

        @Test
        @DisplayName("질문 키워드 포함 여부 체크")
        void checkQuestionKeywords_ShouldReturnIncludedFlags() {
            // Given
            Long questionId = 20L;
            when(questionRepository.existsById(questionId)).thenReturn(true);
            when(questionHashtagRepository.existsByQuestion_IdAndHashtag_NameIgnoreCase(questionId, "redis"))
                    .thenReturn(true);
            when(questionHashtagRepository.existsByQuestion_IdAndHashtag_NameIgnoreCase(questionId, "kafka"))
                    .thenReturn(false);

            // When
            QuestionKeywordCheckResponse result =
                    questionService.checkQuestionKeywords(questionId, List.of("redis", "kafka"));

            // Then
            assertThat(result.keywords()).hasSize(2);
            assertThat(result.keywords().get(0).keyword()).isEqualTo("redis");
            assertThat(result.keywords().get(0).included()).isTrue();
            assertThat(result.keywords().get(1).keyword()).isEqualTo("kafka");
            assertThat(result.keywords().get(1).included()).isFalse();
            verify(questionRepository).existsById(questionId);
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionFlowTest {

        @Test
        @DisplayName("오늘의 추천 질문 조회 시 활성 질문이 없으면 QuestionNotFoundException")
        void getDailyRecommendation_WithoutActiveQuestion_ShouldThrowException() {
            // Given
            when(questionRepository.findRandomActiveId()).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> questionService.getDailyRecommendation())
                    .isInstanceOf(QuestionNotFoundException.class);
        }

        @Test
        @DisplayName("오늘의 추천 질문 ID는 있으나 상세 조회 실패 시 QuestionNotFoundException")
        void getDailyRecommendation_WithMissingQuestionDetail_ShouldThrowException() {
            // Given
            when(questionRepository.findRandomActiveId()).thenReturn(Optional.of(123L));
            when(questionRepository.findById(123L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> questionService.getDailyRecommendation())
                    .isInstanceOf(QuestionNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 질문의 키워드 조회 시 QuestionNotFoundException")
        void getQuestionKeywords_WithUnknownQuestion_ShouldThrowException() {
            // Given
            when(questionRepository.existsById(404L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> questionService.getQuestionKeywords(404L))
                    .isInstanceOf(QuestionNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 질문의 키워드 체크 시 QuestionNotFoundException")
        void checkQuestionKeywords_WithUnknownQuestion_ShouldThrowException() {
            // Given
            when(questionRepository.existsById(404L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> questionService.checkQuestionKeywords(404L, List.of("redis")))
                    .isInstanceOf(QuestionNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 질문 수정 시 QuestionNotFoundException")
        void updateQuestion_WithUnknownQuestion_ShouldThrowException() {
            // Given
            Long questionId = 999L;
            QuestionUpdateRequest request = new QuestionUpdateRequest(
                    "수정 내용",
                    null,
                    null,
                    null,
                    null
            );
            when(questionRepository.findById(questionId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> questionService.updateQuestion(questionId, request))
                    .isInstanceOf(QuestionNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 질문 삭제 시 QuestionNotFoundException")
        void deleteQuestion_WithUnknownQuestion_ShouldThrowException() {
            // Given
            Long questionId = 999L;
            when(questionRepository.findById(questionId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> questionService.deleteQuestion(questionId))
                    .isInstanceOf(QuestionNotFoundException.class);
        }
    }

    private Question mockQuestion(Long id, String content, QuestionType type, QuestionCategory category) {
        Question question = mock(Question.class);
        when(question.getId()).thenReturn(id);
        when(question.getContent()).thenReturn(content);
        when(question.getType()).thenReturn(type);
        when(question.getCategory()).thenReturn(category);
        return question;
    }

    private QuestionHashtag questionHashtag(String keyword) {
        QuestionHashtag questionHashtag = mock(QuestionHashtag.class);
        Hashtag hashtag = mock(Hashtag.class);
        when(hashtag.getName()).thenReturn(keyword);
        when(questionHashtag.getHashtag()).thenReturn(hashtag);
        return questionHashtag;
    }

    private QuestionKeywordRow keywordRow(Long questionId, String keyword) {
        return new QuestionKeywordRow() {
            @Override
            public Long getQuestionId() {
                return questionId;
            }

            @Override
            public String getKeyword() {
                return keyword;
            }
        };
    }
}
