package com.ktb.user.service;

import com.ktb.user.dto.response.LearningStatsResponse;

public interface LearningRecordService {
    LearningStatsResponse getStats(Long accountId);

}
