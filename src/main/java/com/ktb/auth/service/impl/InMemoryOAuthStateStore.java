package com.ktb.auth.service.impl;

import com.ktb.auth.exception.oauth.InvalidStateException;
import com.ktb.auth.service.OAuthStateStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InMemoryOAuthStateStore implements OAuthStateStore {

    private static final long TTL_MS = TimeUnit.MINUTES.toMillis(5);
    private final Map<String, Long> store = new ConcurrentHashMap<>();

    @Override
    public String generateAndStore(String provider) {
        String state = UUID.randomUUID().toString();
        store.put(state, System.currentTimeMillis());
        log.info("State 생성 (InMemory): provider={}, state={}", provider, state);
        return state;
    }

    @Override
    public void validateAndConsume(String state) {
        Long ts = store.remove(state);
        if (ts == null) {
            throw new InvalidStateException();
        }
        if ((System.currentTimeMillis() - ts) > TTL_MS) {
            throw new InvalidStateException();
        }
    }
}