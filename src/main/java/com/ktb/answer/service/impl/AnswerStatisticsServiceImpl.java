package com.ktb.answer.service.impl;

import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.dto.CategoryCount;
import com.ktb.answer.dto.DailyCount;
import com.ktb.answer.dto.TypeCount;
import com.ktb.answer.repository.AnswerRepository;
import com.ktb.answer.service.AnswerStatisticsService;
import com.ktb.user.dto.response.CategoryDistributionResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerStatisticsServiceImpl implements AnswerStatisticsService {

    private final AnswerRepository answerRepository;

    @Override
    public List<TypeCount> getTypeCounts(Long accountId, List<AnswerType> types) {
        log.debug("getTypeCounts - accountId={}, typeFilters={}", accountId, types);
        List<TypeCount> counts = answerRepository.countByAccountIdAndTypeIn(accountId, types);
        log.debug("getTypeCounts completed - accountId={}, resultSize={}", accountId, counts.size());
        return counts;
    }

    @Override
    public List<CategoryDistributionResponse> getCategoryDistribution(Long accountId) {
        log.debug("getCategoryDistribution - accountId={}", accountId);
        List<CategoryCount> rows = answerRepository.countByAccountIdGroupByCategory(accountId);

        List<CategoryDistributionResponse> distribution = rows.stream()
                .map(row -> new CategoryDistributionResponse(
                        row.category().getCategory(),
                        row.count()
                ))
                .toList();
        log.debug("getCategoryDistribution completed - accountId={}, categoryCount={}",
                accountId, distribution.size());
        return distribution;
    }

    @Override
    public int getStreakDays(Long accountId) {
        log.debug("getStreakDays - accountId={}", accountId);
        List<LocalDateTime> createdAtList = answerRepository.findCreatedAtByAccountIdOrderByCreatedAtDesc(accountId);
        if (createdAtList.isEmpty()) {
            log.debug("getStreakDays completed - accountId={}, streakDays=0", accountId);
            return 0;
        }

        List<LocalDate> dates = createdAtList.stream()
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .toList();

        int streakDays = dates.size();
        log.debug("getStreakDays completed - accountId={}, streakDays={}", accountId, streakDays);
        return streakDays;
    }

    @Override
    public List<DailyCount> getDailyCounts(Long accountId, LocalDateTime start, LocalDateTime end) {
        log.debug("getDailyCounts - accountId={}, start={}, end={}", accountId, start, end);
        List<DailyCount> counts = answerRepository.countByAccountIdGroupByDate(accountId, start, end);
        log.debug("getDailyCounts completed - accountId={}, resultSize={}", accountId, counts.size());
        return counts;
    }
}
