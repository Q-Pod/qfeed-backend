package com.ktb.abuse.core;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AbuseCheckContext {

    private final Long accountId;
    private final Long questionId;
    private final String clientIp;
    private final String content;

    public static AbuseCheckContext of(Long accountId, Long questionId, String clientIp, String content) {
        return AbuseCheckContext.builder()
                .accountId(accountId)
                .questionId(questionId)
                .clientIp(clientIp)
                .content(content)
                .build();
    }
}
