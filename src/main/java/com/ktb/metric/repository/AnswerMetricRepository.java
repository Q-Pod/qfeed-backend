package com.ktb.metric.repository;

import com.ktb.metric.domain.AnswerMetric;
import com.ktb.metric.domain.AnswerMetricId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerMetricRepository extends JpaRepository<AnswerMetric, AnswerMetricId> {

    @Query("""
            SELECT am FROM AnswerMetric am
            JOIN FETCH am.id.metric m
            WHERE am.id.answer.id = :answerId
            ORDER BY m.id ASC
            """)
    List<AnswerMetric> findByAnswerIdWithMetric(@Param("answerId") Long answerId);
}
