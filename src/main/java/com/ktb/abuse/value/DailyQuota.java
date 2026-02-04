package com.ktb.abuse.value;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

public record DailyQuota(LocalDate date, AtomicInteger count) {
}
