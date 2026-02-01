package com.ktb.user.service.impl;

import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.dto.DailyCount;
import com.ktb.user.dto.response.CategoryDistributionResponse;
import com.ktb.user.dto.response.LearningStatsResponse;
import com.ktb.user.dto.response.WeeklyStatsResponse;
import com.ktb.user.dto.response.WeeklyStatsResponse.DailyStatEntry;
import com.ktb.answer.service.AnswerStatisticsService;
import com.ktb.answer.dto.TypeCount;
import com.ktb.user.service.LearningRecordService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl implements LearningRecordService {

    private final AnswerStatisticsService answerStatisticsService;

    @Override
    public LearningStatsResponse getStats(Long accountId) {
        List<TypeCount> typeCounts = answerStatisticsService.getTypeCounts(
                accountId,
                List.of(AnswerType.PRACTICE_INTERVIEW, AnswerType.REAL_INTERVIEW)
        );


        long practiceModeCount = getTypeCount(typeCounts, AnswerType.PRACTICE_INTERVIEW);
        long realInterviewCount = getTypeCount(typeCounts, AnswerType.REAL_INTERVIEW);

        long totalQuestionsAnswered = practiceModeCount + realInterviewCount;

        List<CategoryDistributionResponse> categoryDistribution = answerStatisticsService.getCategoryDistribution(accountId);
        int streakDays = answerStatisticsService.getStreakDays(accountId);

        return new LearningStatsResponse(
                practiceModeCount,
                realInterviewCount,
                categoryDistribution,
                streakDays,
                totalQuestionsAnswered
        );
    }

    @Override
    public WeeklyStatsResponse getWeeklyStats(Long accountId) {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDateTime start = monday.atStartOfDay();
        LocalDateTime end = monday.plusDays(7).atStartOfDay();

        List<DailyCount> dailyCounts = answerStatisticsService.getDailyCounts(accountId, start, end);

        Map<LocalDate, Long> countMap = dailyCounts.stream()
                .collect(Collectors.toMap(DailyCount::date, DailyCount::count));

        String[] koreanDays = {"월", "화", "수", "목", "금", "토", "일"};

        List<DailyStatEntry> dailyStats = new ArrayList<>();
        long total = 0;
        long max = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            long count = countMap.getOrDefault(date, 0L);
            dailyStats.add(new DailyStatEntry(date, koreanDays[i], count));
            total += count;
            if (count > max) {
                max = count;
            }
        }

        long maxValueForChart = Math.max(max, 3);
        String weekSummary = "이번주 연습 " + total + "회";

        return new WeeklyStatsResponse(weekSummary, dailyStats, maxValueForChart, total);
    }

    private long getTypeCount(List<TypeCount> rows, AnswerType type) {
        return rows.stream()
                .filter(row -> row.type() == type)
                .mapToLong(TypeCount::count)
                .findFirst()
                .orElse(0L);
    }
}
