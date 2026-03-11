package com.ktb.redis.policy;

import com.ktb.redis.constant.CacheNames;
import lombok.Getter;

import java.time.Duration;

@Getter
public enum CachePolicy {
    EXAMPLE_POLICY(CacheNames.EXAMPLE_CACHE_NAME, Duration.ofMinutes(10));

    private final String cacheName;
    private final Duration ttl;

    CachePolicy(String cacheName, Duration ttl) {
        this.cacheName = cacheName;
        this.ttl = ttl;
    }
}
