package com.ktb.answer.service;

import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.dto.CategoryCount;
import com.ktb.answer.dto.DailyCount;
import com.ktb.answer.dto.TypeCount;
import com.ktb.answer.repository.AnswerRepository;
import com.ktb.answer.service.impl.AnswerStatisticsServiceImpl;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.user.dto.response.CategoryDistributionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerStatisticsService 단위 테스트")
class AnswerStatisticsServiceTest {

    @Mock
    private AnswerRepository answerRepository;

    @InjectMocks
    private AnswerStatisticsServiceImpl answerStatisticsService;

    private static final Long ACCOUNT_ID = 1L;

    @Nested
    @DisplayName("getTypeCounts() 테스트")
    class GetTypeCountsTest {

        @Test
        @DisplayName("답변 타입별 카운트 조회 성공")
        void getTypeCounts_WithAnswers_ShouldReturnCounts() {
            // Given
            List<AnswerType> types = List.of(AnswerType.PRACTICE_INTERVIEW, AnswerType.REAL_INTERVIEW);
            List<TypeCount> expectedCounts = List.of(
                    new TypeCount(AnswerType.PRACTICE_INTERVIEW, 10),
                    new TypeCount(AnswerType.REAL_INTERVIEW, 5)
            );

            when(answerRepository.countByAccountIdAndTypeIn(ACCOUNT_ID, types))
                    .thenReturn(expectedCounts);

            // When
            List<TypeCount> result = answerStatisticsService.getTypeCounts(ACCOUNT_ID, types);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(TypeCount::type)
                    .containsExactly(AnswerType.PRACTICE_INTERVIEW, AnswerType.REAL_INTERVIEW);
            assertThat(result).extracting(TypeCount::count)
                    .containsExactly(10L, 5L);
        }

        @Test
        @DisplayName("답변이 없는 경우 빈 목록 반환")
        void getTypeCounts_WithNoAnswers_ShouldReturnEmptyList() {
            // Given
            List<AnswerType> types = List.of(AnswerType.PRACTICE_INTERVIEW);

            when(answerRepository.countByAccountIdAndTypeIn(ACCOUNT_ID, types))
                    .thenReturn(Collections.emptyList());

            // When
            List<TypeCount> result = answerStatisticsService.getTypeCounts(ACCOUNT_ID, types);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("특정 타입 필터로 카운트 조회")
        void getTypeCounts_WithTypeFilter_ShouldReturnFilteredCounts() {
            // Given
            List<AnswerType> types = List.of(AnswerType.PRACTICE_INTERVIEW);
            List<TypeCount> expectedCounts = List.of(
                    new TypeCount(AnswerType.PRACTICE_INTERVIEW, 15)
            );

            when(answerRepository.countByAccountIdAndTypeIn(ACCOUNT_ID, types))
                    .thenReturn(expectedCounts);

            // When
            List<TypeCount> result = answerStatisticsService.getTypeCounts(ACCOUNT_ID, types);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).type()).isEqualTo(AnswerType.PRACTICE_INTERVIEW);
            assertThat(result.get(0).count()).isEqualTo(15L);
        }
    }

    @Nested
    @DisplayName("getCategoryDistribution() 테스트")
    class GetCategoryDistributionTest {

        @Test
        @DisplayName("카테고리별 분포 조회 성공")
        void getCategoryDistribution_WithAnswers_ShouldReturnDistribution() {
            // Given
            List<CategoryCount> categoryCounts = List.of(
                    new CategoryCount(QuestionCategory.DB, 10),
                    new CategoryCount(QuestionCategory.NETWORK, 5),
                    new CategoryCount(QuestionCategory.OS, 3)
            );

            when(answerRepository.countByAccountIdGroupByCategory(ACCOUNT_ID))
                    .thenReturn(categoryCounts);

            // When
            List<CategoryDistributionResponse> result = answerStatisticsService.getCategoryDistribution(ACCOUNT_ID);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(CategoryDistributionResponse::category)
                    .containsExactly(
                            QuestionCategory.DB.getCategory(),
                            QuestionCategory.NETWORK.getCategory(),
                            QuestionCategory.OS.getCategory()
                    );
        }

        @Test
        @DisplayName("답변이 없는 경우 빈 분포 반환")
        void getCategoryDistribution_WithNoAnswers_ShouldReturnEmptyDistribution() {
            // Given
            when(answerRepository.countByAccountIdGroupByCategory(ACCOUNT_ID))
                    .thenReturn(Collections.emptyList());

            // When
            List<CategoryDistributionResponse> result = answerStatisticsService.getCategoryDistribution(ACCOUNT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getStreakDays() 테스트")
    class GetStreakDaysTest {

        @Test
        @DisplayName("답변이 없는 경우 0 반환")
        void getStreakDays_WithNoAnswers_ShouldReturnZero() {
            // Given
            when(answerRepository.findCreatedAtByAccountIdOrderByCreatedAtDesc(ACCOUNT_ID))
                    .thenReturn(Collections.emptyList());

            // When
            int result = answerStatisticsService.getStreakDays(ACCOUNT_ID);

            // Then
            assertThat(result).isZero();
        }

        @Test
        @DisplayName("연속 답변일 수 계산")
        void getStreakDays_WithConsecutiveDays_ShouldReturnStreakCount() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            List<LocalDateTime> createdAtList = List.of(
                    now,
                    now.minusDays(1),
                    now.minusDays(2)
            );

            when(answerRepository.findCreatedAtByAccountIdOrderByCreatedAtDesc(ACCOUNT_ID))
                    .thenReturn(createdAtList);

            // When
            int result = answerStatisticsService.getStreakDays(ACCOUNT_ID);

            // Then
            assertThat(result).isEqualTo(3);
        }

        @Test
        @DisplayName("같은 날 중복 답변은 1일로 계산")
        void getStreakDays_WithSameDayDuplicates_ShouldCountAsOneDay() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            List<LocalDateTime> createdAtList = List.of(
                    now,
                    now.minusHours(2),
                    now.minusHours(4),
                    now.minusDays(1)
            );

            when(answerRepository.findCreatedAtByAccountIdOrderByCreatedAtDesc(ACCOUNT_ID))
                    .thenReturn(createdAtList);

            // When
            int result = answerStatisticsService.getStreakDays(ACCOUNT_ID);

            // Then
            assertThat(result).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getDailyCounts() 테스트")
    class GetDailyCountsTest {

        @Test
        @DisplayName("기간 내 일별 카운트 조회 성공")
        void getDailyCounts_WithinRange_ShouldReturnDailyCounts() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusDays(7);
            LocalDateTime end = LocalDateTime.now();
            List<DailyCount> expectedCounts = List.of(
                    new DailyCount(LocalDate.now().minusDays(2), 3),
                    new DailyCount(LocalDate.now().minusDays(1), 5),
                    new DailyCount(LocalDate.now(), 2)
            );

            when(answerRepository.countByAccountIdGroupByDate(ACCOUNT_ID, start, end))
                    .thenReturn(expectedCounts);

            // When
            List<DailyCount> result = answerStatisticsService.getDailyCounts(ACCOUNT_ID, start, end);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(DailyCount::count)
                    .containsExactly(3L, 5L, 2L);
        }

        @Test
        @DisplayName("기간 내 답변이 없는 경우 빈 목록 반환")
        void getDailyCounts_WithNoAnswersInRange_ShouldReturnEmptyList() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusDays(7);
            LocalDateTime end = LocalDateTime.now();

            when(answerRepository.countByAccountIdGroupByDate(ACCOUNT_ID, start, end))
                    .thenReturn(Collections.emptyList());

            // When
            List<DailyCount> result = answerStatisticsService.getDailyCounts(ACCOUNT_ID, start, end);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
