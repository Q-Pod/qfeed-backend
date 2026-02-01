package com.ktb.user.service.impl;

import com.ktb.answer.domain.AnswerType;
import com.ktb.user.dto.response.CategoryDistributionResponse;
import com.ktb.user.dto.response.LearningStatsResponse;
import com.ktb.answer.service.AnswerStatisticsService;
import com.ktb.answer.dto.TypeCount;
import com.ktb.user.service.LearningRecordService;
import java.util.List;
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

    private long getTypeCount(List<TypeCount> rows, AnswerType type) {
        return rows.stream()
                .filter(row -> row.type() == type)
                .mapToLong(TypeCount::count)
                .findFirst()
                .orElse(0L);
    }
}
