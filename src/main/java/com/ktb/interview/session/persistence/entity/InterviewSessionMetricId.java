package com.ktb.interview.session.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 세션 메트릭 복합키(session_id + metric_id).
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class InterviewSessionMetricId implements Serializable {

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "metric_id", nullable = false)
    private Long metricId;
}
