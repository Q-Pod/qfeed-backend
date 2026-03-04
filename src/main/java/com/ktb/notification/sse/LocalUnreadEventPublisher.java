package com.ktb.notification.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalUnreadEventPublisher implements UnreadEventPublisher {

    private final SseEmitterRegistry sseEmitterRegistry;

    @Override
    public void publish(Long accountId, boolean hasUnread) {
        sseEmitterRegistry.send(accountId, hasUnread);
    }
}
