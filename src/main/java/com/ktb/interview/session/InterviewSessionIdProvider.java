package com.ktb.interview.session;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 인터뷰 세션 식별자 생성기.
 */
@Component
public class InterviewSessionIdProvider {

    /**
     * 외부 노출 가능한 무작위 세션 ID를 생성합니다.
     */
    public String nextId() {
        return UUID.randomUUID().toString();
    }
}
