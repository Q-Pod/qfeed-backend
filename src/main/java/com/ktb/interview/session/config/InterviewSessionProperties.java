package com.ktb.interview.session.config;

import com.ktb.interview.session.exception.InterviewSessionInvalidConfigException;
import java.time.Duration;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 인터뷰 세션 TTL 설정값 바인딩.
 */
@Component
@Getter
public class InterviewSessionProperties {

    private static final String ERROR_REAL_MAX_TURN_MUST_BE_POSITIVE =
            "interview.session.real-max-turn must be greater than 0";

    private final Duration practiceTtl;
    private final Duration realTtl;
    private final int realMaxTurn;

    /**
     * 연습/실전 세션 TTL 및 실전 최대 turn 설정을 주입받습니다.
     */
    public InterviewSessionProperties(
            @Value("${interview.session.practice-ttl}") Duration practiceTtl,
            @Value("${interview.session.real-ttl}") Duration realTtl,
            @Value("${interview.session.real-max-turn}") int realMaxTurn
    ) {
        if (realMaxTurn <= 0) {
            throw new InterviewSessionInvalidConfigException(ERROR_REAL_MAX_TURN_MUST_BE_POSITIVE);
        }
        this.practiceTtl = practiceTtl;
        this.realTtl = realTtl;
        this.realMaxTurn = realMaxTurn;
    }
}
