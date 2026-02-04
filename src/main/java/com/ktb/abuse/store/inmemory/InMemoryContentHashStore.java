package com.ktb.abuse.store.inmemory;

import com.ktb.abuse.store.ContentHashStore;
import com.ktb.abuse.value.HashEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!redis")
public class InMemoryContentHashStore implements ContentHashStore {

    private static final int MAX_STORED_HASHES = 100;

    private final ConcurrentHashMap<String, HashEntry> exactHashes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> simHashes = new ConcurrentHashMap<>();

    @Override
    public void storeHash(Long accountId, Long questionId, String hash, String simHash) {
        String key = buildKey(accountId, questionId);

        exactHashes.put(key + ":" + hash, new HashEntry(hash, System.currentTimeMillis()));

        simHashes.compute(key, (k, existing) -> {
            List<String> list = ((existing == null) ? new ArrayList<>() : new ArrayList<>(existing));
            list.add(simHash);
            if (list.size() > MAX_STORED_HASHES) {
                list = list.subList(list.size() - MAX_STORED_HASHES, list.size());
            }
            return list;
        });
    }

    @Override
    public boolean existsHash(Long accountId, Long questionId, String hash) {
        String key = buildKey(accountId, questionId) + ":" + hash;
        return exactHashes.containsKey(key);
    }

    @Override
    public List<String> getRecentSimHashes(Long accountId, Long questionId, int limit) {
        String key = buildKey(accountId, questionId);
        List<String> hashes = simHashes.get(key);
        if (hashes == null || hashes.isEmpty()) {
            return Collections.emptyList();
        }
        int fromIndex = Math.max(0, hashes.size() - limit);
        return new ArrayList<>(hashes.subList(fromIndex, hashes.size()));
    }

    private String buildKey(Long accountId, Long questionId) {
        return accountId + ":" + questionId;
    }
}
