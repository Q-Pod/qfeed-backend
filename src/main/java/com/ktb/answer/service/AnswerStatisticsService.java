package com.ktb.answer.service;

import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.dto.DailyCount;
import com.ktb.answer.dto.TypeCount;
import com.ktb.user.dto.response.CategoryDistributionResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface AnswerStatisticsService {

    /**
     * 답변 유형별 개수를 조회합니다.
     *
     * @param accountId 사용자 ID
     * @param types     조회할 답변 유형 목록
     * @return 유형별 답변 개수 목록
     */
    List<TypeCount> getTypeCounts(Long accountId, List<AnswerType> types);


    /**
     * 사용자의 카테고리별 학습 분포를 조회합니다.
     *
     * @param accountId 사용자 ID
     * @return 카테고리별 학습 횟수 목록
     */
    List<CategoryDistributionResponse> getCategoryDistribution(Long accountId);


    /**
     * 사용자의 연속 학습 일수를 조회합니다.
     *
     * @param accountId 사용자 ID
     * @return 연속 학습 일수 (답변 기록이 없으면 0)
     */
    int getStreakDays(Long accountId);

    /**
     * 기간 내 일별 답변 개수를 조회합니다.
     *
     * @param accountId 사용자 ID
     * @param start     조회 시작 일시 (포함)
     * @param end       조회 종료 일시 (포함)
     * @return 일별 답변 개수 목록
     */
    List<DailyCount> getDailyCounts(Long accountId, LocalDateTime start, LocalDateTime end);
}
