package com.ktb.interview.session.config;

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

    private final Duration practiceTtl;
    private final Duration realTtl;

    /**
     * 연습/실전 세션 TTL 설정을 주입받습니다.
     */
    public InterviewSessionProperties(
            @Value("${interview.session.practice-ttl}") Duration practiceTtl,
            @Value("${interview.session.real-ttl}") Duration realTtl
    ) {
        this.practiceTtl = practiceTtl;
        this.realTtl = realTtl;
    }
}
