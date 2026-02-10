package com.ktb.answer.repository;

import com.ktb.answer.domain.Answer;
import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.dto.CategoryCount;
import com.ktb.answer.dto.DailyCount;
import com.ktb.answer.dto.TypeCount;
import com.ktb.auth.domain.UserAccount;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
@DisplayName("AnswerRepository 통합 테스트")
class AnswerRepositoryTest {

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private UserAccount testAccount;
    private UserAccount otherAccount;
    private Question csQuestion;
    private Question networkQuestion;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testAccount = UserAccount.createEmailAccount("test@example.com", "테스트유저");
        testEntityManager.persist(testAccount);

        otherAccount = UserAccount.createEmailAccount("other@example.com", "다른유저");
        testEntityManager.persist(otherAccount);

        // 테스트 질문 생성
        csQuestion = Question.create(
                "운영체제의 프로세스와 스레드의 차이를 설명하세요.",
                QuestionType.CS,
                QuestionCategory.OS
        );
        testEntityManager.persist(csQuestion);

        networkQuestion = Question.create(
                "TCP와 UDP의 차이점을 설명하세요.",
                QuestionType.CS,
                QuestionCategory.NETWORK
        );
        testEntityManager.persist(networkQuestion);

        testEntityManager.clear();
    }

    @Nested
    @DisplayName("findByAccountIdWithFiltersNoCursor() 테스트 - 커서 없는 조회")
    class FindByAccountIdWithFiltersNoCursorTest {

        @Test
        @DisplayName("첫 페이지 정상 조회")
        void findFirstPage_ShouldReturnAnswers() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            createAndSaveAnswer(testAccount, networkQuestion, AnswerType.PRACTICE_INTERVIEW);

            LocalDateTime dateFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

            // When
            Slice<Answer> result = answerRepository.findByAccountIdWithFiltersNoCursor(
                    testAccount.getId(),
                    null,
                    null,
                    null,
                    dateFrom,
                    dateTo,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("빈 결과 조회")
        void findWithNoResults_ShouldReturnEmptySlice() {
            // Given
            LocalDateTime dateFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

            // When
            Slice<Answer> result = answerRepository.findByAccountIdWithFiltersNoCursor(
                    testAccount.getId(),
                    null,
                    null,
                    null,
                    dateFrom,
                    dateTo,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("다른 사용자 답변은 조회되지 않음")
        void findByAccountId_ShouldNotIncludeOtherUserAnswers() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            createAndSaveAnswer(otherAccount, networkQuestion, AnswerType.PRACTICE_INTERVIEW);

            LocalDateTime dateFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

            // When
            Slice<Answer> result = answerRepository.findByAccountIdWithFiltersNoCursor(
                    testAccount.getId(),
                    null,
                    null,
                    null,
                    dateFrom,
                    dateTo,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getAccount().getId())
                    .isEqualTo(testAccount.getId());
        }

        @Test
        @DisplayName("type 필터 적용")
        void findWithTypeFilter_ShouldFilterByType() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            createAndSaveAnswer(testAccount, networkQuestion, AnswerType.REAL_INTERVIEW);

            LocalDateTime dateFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

            // When
            Slice<Answer> result = answerRepository.findByAccountIdWithFiltersNoCursor(
                    testAccount.getId(),
                    AnswerType.PRACTICE_INTERVIEW,
                    null,
                    null,
                    dateFrom,
                    dateTo,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getType())
                    .isEqualTo(AnswerType.PRACTICE_INTERVIEW);
        }

        @Test
        @DisplayName("category 필터 적용")
        void findWithCategoryFilter_ShouldFilterByCategory() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            createAndSaveAnswer(testAccount, networkQuestion, AnswerType.PRACTICE_INTERVIEW);

            LocalDateTime dateFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

            // When
            Slice<Answer> result = answerRepository.findByAccountIdWithFiltersNoCursor(
                    testAccount.getId(),
                    null,
                    QuestionCategory.OS,
                    null,
                    dateFrom,
                    dateTo,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getQuestion().getCategory())
                    .isEqualTo(QuestionCategory.OS);
        }

        @Test
        @DisplayName("questionType 필터 적용")
        void findWithQuestionTypeFilter_ShouldFilterByQuestionType() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);

            LocalDateTime dateFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

            // When
            Slice<Answer> result = answerRepository.findByAccountIdWithFiltersNoCursor(
                    testAccount.getId(),
                    null,
                    null,
                    QuestionType.CS,
                    dateFrom,
                    dateTo,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getQuestion().getType())
                    .isEqualTo(QuestionType.CS);
        }

        @Test
        @DisplayName("hasNext가 true일 때 더 많은 데이터 존재")
        void findWithHasNextTrue_ShouldIndicateMoreData() {
            // Given
            for (int i = 0; i < 5; i++) {
                createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            }

            LocalDateTime dateFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

            // When
            Slice<Answer> result = answerRepository.findByAccountIdWithFiltersNoCursor(
                    testAccount.getId(),
                    null,
                    null,
                    null,
                    dateFrom,
                    dateTo,
                    PageRequest.of(0, 3)
            );

            // Then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.hasNext()).isTrue();
        }

        @Test
        @DisplayName("마지막 페이지에서 hasNext가 false")
        void findLastPage_ShouldHaveNoNext() {
            // Given
            for (int i = 0; i < 3; i++) {
                createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            }

            LocalDateTime dateFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

            // When
            Slice<Answer> result = answerRepository.findByAccountIdWithFiltersNoCursor(
                    testAccount.getId(),
                    null,
                    null,
                    null,
                    dateFrom,
                    dateTo,
                    PageRequest.of(0, 5)
            );

            // Then
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("삭제된 답변은 조회되지 않음")
        void findWithDeletedAnswer_ShouldExcludeDeleted() {
            // Given
            Answer activeAnswer = createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            Answer deletedAnswer = createAndSaveAnswer(testAccount, networkQuestion, AnswerType.PRACTICE_INTERVIEW);

            // 답변 삭제
            deletedAnswer.softDelete();
            testEntityManager.flush();
            testEntityManager.clear();

            LocalDateTime dateFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

            // When
            Slice<Answer> result = answerRepository.findByAccountIdWithFiltersNoCursor(
                    testAccount.getId(),
                    null,
                    null,
                    null,
                    dateFrom,
                    dateTo,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getId()).isEqualTo(activeAnswer.getId());
        }

        @Test
        @DisplayName("날짜 범위 필터 적용")
        void findWithDateRange_ShouldFilterByDateRange() {
            // Given
            Answer answer = createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);

            LocalDateTime dateFrom = LocalDateTime.now().plusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(2);

            // When
            Slice<Answer> result = answerRepository.findByAccountIdWithFiltersNoCursor(
                    testAccount.getId(),
                    null,
                    null,
                    null,
                    dateFrom,
                    dateTo,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByAccountIdWithFilters() 테스트 - 커서 기반 조회")
    class FindByAccountIdWithFiltersTest {

        @Test
        @DisplayName("커서 이후 데이터 조회")
        void findAfterCursor_ShouldReturnAnswersAfterCursor() throws InterruptedException {
            // Given
            Answer answer1 = createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            Thread.sleep(10); // createdAt 차이를 위해 잠시 대기
            Answer answer2 = createAndSaveAnswer(testAccount, networkQuestion, AnswerType.PRACTICE_INTERVIEW);
            Thread.sleep(10);
            Answer answer3 = createAndSaveAnswer(testAccount, csQuestion, AnswerType.REAL_INTERVIEW);

            testEntityManager.flush();
            testEntityManager.clear();

            // answer2를 다시 조회하여 createdAt 가져오기
            Answer cursorAnswer = answerRepository.findById(answer2.getId()).orElseThrow();

            LocalDateTime dateFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

            // When - answer2 이후의 데이터 조회 (answer1만 해당)
            Slice<Answer> result = answerRepository.findByAccountIdWithFilters(
                    testAccount.getId(),
                    null,
                    null,
                    null,
                    dateFrom,
                    dateTo,
                    cursorAnswer.getCreatedAt(),
                    cursorAnswer.getId(),
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getId()).isEqualTo(answer1.getId());
        }

        @Test
        @DisplayName("동일한 createdAt을 가진 답변들의 정렬 (id 기준)")
        void findWithSameCreatedAt_ShouldSortById() {
            // Given - 동일 시간대에 여러 답변 생성
            Answer answer1 = createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            Answer answer2 = createAndSaveAnswer(testAccount, networkQuestion, AnswerType.PRACTICE_INTERVIEW);
            Answer answer3 = createAndSaveAnswer(testAccount, csQuestion, AnswerType.REAL_INTERVIEW);

            LocalDateTime dateFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

            // When
            Slice<Answer> result = answerRepository.findByAccountIdWithFiltersNoCursor(
                    testAccount.getId(),
                    null,
                    null,
                    null,
                    dateFrom,
                    dateTo,
                    PageRequest.of(0, 10)
            );

            // Then - id 내림차순 정렬 확인
            List<Answer> answers = result.getContent();
            assertThat(answers).hasSize(3);

            for (int i = 0; i < answers.size() - 1; i++) {
                Answer current = answers.get(i);
                Answer next = answers.get(i + 1);

                // createdAt 내림차순, 같으면 id 내림차순
                if (current.getCreatedAt().equals(next.getCreatedAt())) {
                    assertThat(current.getId()).isGreaterThan(next.getId());
                } else {
                    assertThat(current.getCreatedAt()).isAfterOrEqualTo(next.getCreatedAt());
                }
            }
        }
    }

    @Nested
    @DisplayName("findByIdWithQuestion() 테스트")
    class FindByIdWithQuestionTest {

        @Test
        @DisplayName("답변과 질문 함께 조회")
        void findByIdWithQuestion_ShouldReturnAnswerWithQuestion() {
            // Given
            Answer answer = createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            testEntityManager.flush();
            testEntityManager.clear();

            // When
            Answer result = answerRepository.findByIdWithQuestion(answer.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getQuestion()).isNotNull();
            assertThat(result.getQuestion().getId()).isEqualTo(csQuestion.getId());
        }

        @Test
        @DisplayName("존재하지 않는 답변 조회 시 null 반환")
        void findByIdWithQuestion_WithNonExistent_ShouldReturnNull() {
            // Given & When
            Answer result = answerRepository.findByIdWithQuestion(999L);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("삭제된 답변 조회 시 null 반환")
        void findByIdWithQuestion_WithDeleted_ShouldReturnNull() {
            // Given
            Answer answer = createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            answer.softDelete();
            testEntityManager.flush();
            testEntityManager.clear();

            // When
            Answer result = answerRepository.findByIdWithQuestion(answer.getId());

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("countByAccountIdAndTypeIn() 테스트")
    class CountByAccountIdAndTypeInTest {

        @Test
        @DisplayName("타입별 답변 수 집계")
        void countByType_ShouldReturnTypeCounts() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            createAndSaveAnswer(testAccount, networkQuestion, AnswerType.PRACTICE_INTERVIEW);
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.REAL_INTERVIEW);

            // When
            List<TypeCount> result = answerRepository.countByAccountIdAndTypeIn(
                    testAccount.getId(),
                    List.of(AnswerType.PRACTICE_INTERVIEW, AnswerType.REAL_INTERVIEW)
            );

            // Then
            assertThat(result).hasSize(2);

            TypeCount practiceCount = result.stream()
                    .filter(tc -> tc.type() == AnswerType.PRACTICE_INTERVIEW)
                    .findFirst()
                    .orElseThrow();
            assertThat(practiceCount.count()).isEqualTo(2);

            TypeCount realCount = result.stream()
                    .filter(tc -> tc.type() == AnswerType.REAL_INTERVIEW)
                    .findFirst()
                    .orElseThrow();
            assertThat(realCount.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("삭제된 답변은 카운트에서 제외")
        void countByType_ShouldExcludeDeleted() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            Answer deletedAnswer = createAndSaveAnswer(testAccount, networkQuestion, AnswerType.PRACTICE_INTERVIEW);
            deletedAnswer.softDelete();
            testEntityManager.flush();
            testEntityManager.clear();

            // When
            List<TypeCount> result = answerRepository.countByAccountIdAndTypeIn(
                    testAccount.getId(),
                    List.of(AnswerType.PRACTICE_INTERVIEW)
            );

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("답변이 없을 때 빈 리스트 반환")
        void countByType_WithNoAnswers_ShouldReturnEmptyList() {
            // Given & When
            List<TypeCount> result = answerRepository.countByAccountIdAndTypeIn(
                    testAccount.getId(),
                    List.of(AnswerType.PRACTICE_INTERVIEW)
            );

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByAccountIdGroupByCategory() 테스트")
    class CountByAccountIdGroupByCategoryTest {

        @Test
        @DisplayName("카테고리별 답변 수 집계")
        void countByCategory_ShouldReturnCategoryCounts() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.REAL_INTERVIEW);
            createAndSaveAnswer(testAccount, networkQuestion, AnswerType.PRACTICE_INTERVIEW);

            // When
            List<CategoryCount> result = answerRepository.countByAccountIdGroupByCategory(
                    testAccount.getId()
            );

            // Then
            assertThat(result).hasSize(2);

            CategoryCount osCount = result.stream()
                    .filter(cc -> cc.category() == QuestionCategory.OS)
                    .findFirst()
                    .orElseThrow();
            assertThat(osCount.count()).isEqualTo(2);

            CategoryCount networkCount = result.stream()
                    .filter(cc -> cc.category() == QuestionCategory.NETWORK)
                    .findFirst()
                    .orElseThrow();
            assertThat(networkCount.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("답변이 없을 때 빈 리스트 반환")
        void countByCategory_WithNoAnswers_ShouldReturnEmptyList() {
            // Given & When
            List<CategoryCount> result = answerRepository.countByAccountIdGroupByCategory(
                    testAccount.getId()
            );

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByAccountIdGroupByDate() 테스트")
    class CountByAccountIdGroupByDateTest {

        @Test
        @DisplayName("일별 답변 수 집계")
        void countByDate_ShouldReturnDailyCounts() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            createAndSaveAnswer(testAccount, networkQuestion, AnswerType.PRACTICE_INTERVIEW);

            LocalDateTime startDateTime = LocalDateTime.now().minusDays(1);
            LocalDateTime endDateTime = LocalDateTime.now().plusDays(1);

            // When
            List<DailyCount> result = answerRepository.countByAccountIdGroupByDate(
                    testAccount.getId(),
                    startDateTime,
                    endDateTime
            );

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().count()).isEqualTo(2);
        }

        @Test
        @DisplayName("날짜 범위 외 답변은 제외")
        void countByDate_ShouldExcludeOutOfRange() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);

            LocalDateTime startDateTime = LocalDateTime.now().plusDays(1);
            LocalDateTime endDateTime = LocalDateTime.now().plusDays(2);

            // When
            List<DailyCount> result = answerRepository.countByAccountIdGroupByDate(
                    testAccount.getId(),
                    startDateTime,
                    endDateTime
            );

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("삭제된 답변은 카운트에서 제외")
        void countByDate_ShouldExcludeDeleted() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            Answer deletedAnswer = createAndSaveAnswer(testAccount, networkQuestion, AnswerType.PRACTICE_INTERVIEW);
            deletedAnswer.softDelete();
            testEntityManager.flush();
            testEntityManager.clear();

            LocalDateTime startDateTime = LocalDateTime.now().minusDays(1);
            LocalDateTime endDateTime = LocalDateTime.now().plusDays(1);

            // When
            List<DailyCount> result = answerRepository.countByAccountIdGroupByDate(
                    testAccount.getId(),
                    startDateTime,
                    endDateTime
            );

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findCreatedAtByAccountIdOrderByCreatedAtDesc() 테스트")
    class FindCreatedAtByAccountIdTest {

        @Test
        @DisplayName("생성일시 내림차순으로 조회")
        void findCreatedAt_ShouldReturnDescOrder() throws InterruptedException {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            Thread.sleep(10);
            createAndSaveAnswer(testAccount, networkQuestion, AnswerType.PRACTICE_INTERVIEW);
            Thread.sleep(10);
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.REAL_INTERVIEW);

            // When
            List<LocalDateTime> result = answerRepository.findCreatedAtByAccountIdOrderByCreatedAtDesc(
                    testAccount.getId()
            );

            // Then
            assertThat(result).hasSize(3);

            for (int i = 0; i < result.size() - 1; i++) {
                assertThat(result.get(i)).isAfterOrEqualTo(result.get(i + 1));
            }
        }

        @Test
        @DisplayName("삭제된 답변은 제외")
        void findCreatedAt_ShouldExcludeDeleted() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            Answer deletedAnswer = createAndSaveAnswer(testAccount, networkQuestion, AnswerType.PRACTICE_INTERVIEW);
            deletedAnswer.softDelete();
            testEntityManager.flush();
            testEntityManager.clear();

            // When
            List<LocalDateTime> result = answerRepository.findCreatedAtByAccountIdOrderByCreatedAtDesc(
                    testAccount.getId()
            );

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Null 파라미터 처리 테스트")
    class NullParameterTest {

        @Test
        @DisplayName("type이 null일 때 모든 타입 조회")
        void findWithNullType_ShouldReturnAllTypes() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            createAndSaveAnswer(testAccount, networkQuestion, AnswerType.REAL_INTERVIEW);

            LocalDateTime dateFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

            // When
            Slice<Answer> result = answerRepository.findByAccountIdWithFiltersNoCursor(
                    testAccount.getId(),
                    null,  // type = null
                    null,
                    null,
                    dateFrom,
                    dateTo,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("category가 null일 때 모든 카테고리 조회")
        void findWithNullCategory_ShouldReturnAllCategories() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);
            createAndSaveAnswer(testAccount, networkQuestion, AnswerType.PRACTICE_INTERVIEW);

            LocalDateTime dateFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

            // When
            Slice<Answer> result = answerRepository.findByAccountIdWithFiltersNoCursor(
                    testAccount.getId(),
                    null,
                    null,  // category = null
                    null,
                    dateFrom,
                    dateTo,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("questionType이 null일 때 모든 질문 타입 조회")
        void findWithNullQuestionType_ShouldReturnAllQuestionTypes() {
            // Given
            createAndSaveAnswer(testAccount, csQuestion, AnswerType.PRACTICE_INTERVIEW);

            LocalDateTime dateFrom = LocalDateTime.now().minusDays(1);
            LocalDateTime dateTo = LocalDateTime.now().plusDays(1);

            // When
            Slice<Answer> result = answerRepository.findByAccountIdWithFiltersNoCursor(
                    testAccount.getId(),
                    null,
                    null,
                    null,  // questionType = null
                    dateFrom,
                    dateTo,
                    PageRequest.of(0, 10)
            );

            // Then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    // Helper methods

    private Answer createAndSaveAnswer(UserAccount account, Question question, AnswerType type) {
        Answer answer = Answer.create(
                question,
                account,
                "테스트 답변 내용입니다.",
                type
        );
        Answer saved = testEntityManager.persist(answer);
        testEntityManager.flush();
        return saved;
    }
}
