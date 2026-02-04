package com.ktb.abuse.store;

import java.util.List;

public interface ContentHashStore {

    void storeHash(Long accountId, Long questionId, String hash, String simHash);

    boolean existsHash(Long accountId, Long questionId, String hash);

    List<String> getRecentSimHashes(Long accountId, Long questionId, int limit);
}
