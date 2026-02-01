package com.ktb.answer.dto;

import java.time.LocalDate;

public record DailyCount(
        LocalDate date,
        long count
) {
}
