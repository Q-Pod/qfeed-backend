package com.ktb.auth.service.impl;

import com.ktb.auth.dto.ExchangeCodeEntry;
import com.ktb.auth.dto.OAuthExchangePayload;
import com.ktb.auth.exception.oauth.InvalidExchangeCodeException;
import com.ktb.auth.service.OAuthExchangeCodeStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InMemoryOAuthExchangeCodeStore implements OAuthExchangeCodeStore {

    private static final long TTL_MS = TimeUnit.MINUTES.toMillis(1);
    private final Map<String, ExchangeCodeEntry> store = new ConcurrentHashMap<>();

    @Override
    public String generateAndStore(OAuthExchangePayload payload) {
        String code = UUID.randomUUID().toString();
        store.put(code, new ExchangeCodeEntry(payload, System.currentTimeMillis()));
        log.info("Exchange code 생성 (InMemory): accountId={}, code={}", payload.accountId(), code);
        return code;
    }

    @Override
    public OAuthExchangePayload validateAndConsume(String exchangeCode) {
        ExchangeCodeEntry entry = store.remove(exchangeCode);
        if (entry == null) {
            throw new InvalidExchangeCodeException();
        }
        if ((System.currentTimeMillis() - entry.createdAt()) > TTL_MS) {
            throw new InvalidExchangeCodeException();
        }
        return entry.payload();
    }
}