package com.ktb.abuse.store;

public interface QuotaStore {

    int incrementDailyQuota(Long accountId);

    int getDailyQuota(Long accountId);
}
