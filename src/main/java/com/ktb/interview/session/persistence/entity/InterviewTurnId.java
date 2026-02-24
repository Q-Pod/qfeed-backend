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
 * 인터뷰 턴 복합키(session_id + turn_order).
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class InterviewTurnId implements Serializable {

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "turn_order", nullable = false)
    private Integer turnOrder;
}
