package com.ktb.notification.sse;

public interface UnreadEventPublisher {
    void publish(Long accountId, boolean hasUnread);
}
