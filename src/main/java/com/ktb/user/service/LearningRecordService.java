package com.ktb.user.service;

import com.ktb.user.dto.response.LearningStatsResponse;
import com.ktb.user.dto.response.WeeklyStatsResponse;

public interface LearningRecordService {
    LearningStatsResponse getStats(Long accountId);

    WeeklyStatsResponse getWeeklyStats(Long accountId);
}
