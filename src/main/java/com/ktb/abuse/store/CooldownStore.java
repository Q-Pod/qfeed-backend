package com.ktb.abuse.store;

import java.util.Optional;

public interface CooldownStore {

    void setLastSubmitTime(Long accountId, Long questionId, long timestamp);

    Optional<Long> getLastSubmitTime(Long accountId, Long questionId);

    void incrementConsecutiveCount(Long accountId, Long questionId);

    int getConsecutiveCount(Long accountId, Long questionId);

    void resetConsecutiveCount(Long accountId, Long questionId);
}
