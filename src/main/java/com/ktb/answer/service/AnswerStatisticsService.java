package com.ktb.answer.service;

import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.dto.DailyCount;
import com.ktb.answer.dto.TypeCount;
import com.ktb.user.dto.response.CategoryDistributionResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface AnswerStatisticsService {

    List<TypeCount> getTypeCounts(Long accountId, List<AnswerType> types);

    List<CategoryDistributionResponse> getCategoryDistribution(Long accountId);

    int getStreakDays(Long accountId);

    List<DailyCount> getDailyCounts(Long accountId, LocalDateTime start, LocalDateTime end);
}
