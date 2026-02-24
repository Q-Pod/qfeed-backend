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
 * 토픽 피드백 복합키(session_id + topic_id).
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class InterviewTopicFeedbackId implements Serializable {

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "topic_id", nullable = false)
    private Integer topicId;
}
