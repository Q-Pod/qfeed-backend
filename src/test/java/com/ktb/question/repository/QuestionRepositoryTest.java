package com.ktb.question.repository;

import com.ktb.common.config.JpaAuditingConfig;
import com.ktb.question.domain.Question;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
@DisplayName("QuestionRepository 통합 테스트")
public class QuestionRepositoryTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private Question csOsQuestion;
    private Question csDbQuestion;
    private Question systemDesignQuestion;
    private Question inactiveQuestion;
    private Question deletedQuestion;

    @BeforeEach
    void setUp() {
        csOsQuestion = Question.create(
                "운영체제에서 프로세스와 스레드의 차이를 설명해주세요.",
                QuestionType.CS,
                QuestionCategory.OS
        );
        testEntityManager.persist(csOsQuestion);

        csDbQuestion = Question.create(
                "트랜잭션의 ACID 속성을 설명해주세요.",
                QuestionType.CS,
                QuestionCategory.DB
        );
        testEntityManager.persist(csDbQuestion);

        systemDesignQuestion = Question.create(
                "실시간 채팅 시스템 아키텍처를 설계해보세요.",
                QuestionType.SYSTEM_DESIGN,
                QuestionCategory.REALTIME
        );
        testEntityManager.persist(systemDesignQuestion);

        inactiveQuestion = Question.create(
                "비활성화된 질문",
                QuestionType.CS,
                QuestionCategory.NETWORK
        );
        inactiveQuestion.disable();
        testEntityManager.persist(inactiveQuestion);

        deletedQuestion = Question.create(
                "삭제된 질문",
                QuestionType.SYSTEM_DESIGN,
                QuestionCategory.SEARCH
        );
        deletedQuestion.delete();
        testEntityManager.persist(deletedQuestion);

        testEntityManager.flush();
        testEntityManager.clear();
    }

    @Nested
    @DisplayName("findActiveByFilters() 테스트")
    class FindActiveByFiltersTest {

        @Test
        @DisplayName("필터 없이 조회하면 활성 질문만 ID 내림차순으로 조회")
        void findActiveByFilters_WithoutFilters_ShouldReturnOnlyActiveQuestions() {
            // When
            Slice<Question> result = questionRepository.findActiveByFilters(
                    null,
                    null,
                    null,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent())
                    .allMatch(question -> question.isUseYn() && question.getDeletedAt() == null);
            assertThat(result.getContent())
                    .extracting(Question::getId)
                    .isSortedAccordingTo((a, b) -> Long.compare(b, a));
        }

        @Test
        @DisplayName("type 필터로 SYSTEM_DESIGN 질문만 조회")
        void findActiveByFilters_WithTypeFilter_ShouldFilterByType() {
            // When
            Slice<Question> result = questionRepository.findActiveByFilters(
                    QuestionType.SYSTEM_DESIGN,
                    null,
                    null,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getType()).isEqualTo(QuestionType.SYSTEM_DESIGN);
            assertThat(result.getContent().getFirst().getCategory()).isEqualTo(QuestionCategory.REALTIME);
        }

        @Test
        @DisplayName("category 필터로 DB 질문만 조회")
        void findActiveByFilters_WithCategoryFilter_ShouldFilterByCategory() {
            // When
            Slice<Question> result = questionRepository.findActiveByFilters(
                    null,
                    QuestionCategory.DB,
                    null,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getCategory()).isEqualTo(QuestionCategory.DB);
        }

        @Test
        @DisplayName("cursor 값보다 작은 ID만 조회")
        void findActiveByFilters_WithCursor_ShouldReturnQuestionsBeforeCursor() {
            // Given
            Slice<Question> firstPage = questionRepository.findActiveByFilters(
                    null,
                    null,
                    null,
                    PageRequest.of(0, 10)
            );
            Long cursor = firstPage.getContent().getFirst().getId();

            // When
            Slice<Question> result = questionRepository.findActiveByFilters(
                    null,
                    null,
                    cursor,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).allMatch(question -> question.getId() < cursor);
            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("searchActiveByKeyword() 테스트")
    class SearchActiveByKeywordTest {

        @Test
        @DisplayName("키워드 대소문자 구분 없이 검색")
        void searchActiveByKeyword_ShouldSearchCaseInsensitive() {
            // Given
            Question cacheQuestion = Question.create(
                    "Redis CACHE 전략을 설명해주세요.",
                    QuestionType.SYSTEM_DESIGN,
                    QuestionCategory.STORAGE
            );
            testEntityManager.persist(cacheQuestion);
            testEntityManager.flush();
            testEntityManager.clear();

            // When
            Slice<Question> result = questionRepository.searchActiveByKeyword(
                    "cache",
                    null,
                    null,
                    null,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent())
                    .extracting(Question::getContent)
                    .anyMatch(content -> content.toLowerCase().contains("cache"));
        }

        @Test
        @DisplayName("검색에서도 비활성/삭제 질문은 제외")
        void searchActiveByKeyword_ShouldExcludeInactiveOrDeleted() {
            // When
            Slice<Question> result = questionRepository.searchActiveByKeyword(
                    "질문",
                    null,
                    null,
                    null,
                    PageRequest.of(0, 20)
            );

            // Then
            assertThat(result.getContent())
                    .noneMatch(question ->
                            "비활성화된 질문".equals(question.getContent())
                                    || "삭제된 질문".equals(question.getContent()));
        }
    }

    @Nested
    @DisplayName("findRandomActiveId() 테스트")
    class FindRandomActiveIdTest {

        @Test
        @DisplayName("활성 질문이 있으면 활성 질문 ID 중 하나를 반환")
        void findRandomActiveId_WithActiveQuestions_ShouldReturnActiveId() {
            // Given
            Set<Long> activeIds = questionRepository.findAll().stream()
                    .filter(question -> question.isUseYn() && question.getDeletedAt() == null)
                    .map(Question::getId)
                    .collect(Collectors.toSet());

            // When
            Long randomId = questionRepository.findRandomActiveId().orElse(null);

            // Then
            assertThat(randomId).isNotNull();
            assertThat(activeIds).contains(randomId);
        }

        @Test
        @DisplayName("활성 질문이 없으면 empty 반환")
        void findRandomActiveId_WithoutActiveQuestions_ShouldReturnEmpty() {
            // Given
            List<Question> allQuestions = questionRepository.findAll();
            allQuestions.stream()
                    .filter(question -> question.isUseYn() && question.getDeletedAt() == null)
                    .forEach(Question::delete);
            testEntityManager.flush();
            testEntityManager.clear();

            // When
            var result = questionRepository.findRandomActiveId();

            // Then
            assertThat(result).isEmpty();
        }
    }
}
