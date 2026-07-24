package com.lingko.lingko.api.quota.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * HTTP 경계에서 사용하는 Practice 할당량 요청·응답 구조를 정의한다.
 *
 * 영속 엔티티를 직접 노출하지 않고 전송 계약을 독립적으로 유지하기 위해 전용 DTO를 선택했다.
 */
public record PracticeQuotaResponse(
        LocalDate date,
        int freeLimit,
        int freeUsed,
        int rewardedAvailable,
        int remainingPractices,
        OffsetDateTime resetAt
) {
}
