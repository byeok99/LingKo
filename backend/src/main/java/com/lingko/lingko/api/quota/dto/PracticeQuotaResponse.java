package com.lingko.lingko.api.quota.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PracticeQuotaResponse(
        LocalDate date,
        int freeLimit,
        int freeUsed,
        int rewardedAvailable,
        int remainingPractices,
        OffsetDateTime resetAt
) {
}
