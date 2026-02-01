package com.ktb.answer.service.impl;

import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.dto.CategoryCount;
import com.ktb.answer.dto.TypeCount;
import com.ktb.answer.repository.AnswerRepository;
import com.ktb.answer.service.AnswerStatisticsService;
import com.ktb.user.dto.response.CategoryDistributionResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnswerStatisticsServiceImpl implements AnswerStatisticsService {

    private final AnswerRepository answerRepository;

    @Override
    public List<TypeCount> getTypeCounts(Long accountId, List<AnswerType> types) {
        return answerRepository.countByAccountIdAndTypeIn(accountId, types);
    }

    @Override
    public List<CategoryDistributionResponse> getCategoryDistribution(Long accountId) {
        List<CategoryCount> rows = answerRepository.countByAccountIdGroupByCategory(accountId);

        return rows.stream()
                .map(row -> new CategoryDistributionResponse(
                        row.category().getCategory(),
                        row.count()
                ))
                .toList();
    }

    @Override
    public int getStreakDays(Long accountId) {
        List<LocalDateTime> createdAtList = answerRepository.findCreatedAtByAccountIdOrderByCreatedAtDesc(accountId);
        if (createdAtList.isEmpty()) {
            return 0;
        }

        List<LocalDate> dates = createdAtList.stream()
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .toList();

        return dates.size();
    }
}
