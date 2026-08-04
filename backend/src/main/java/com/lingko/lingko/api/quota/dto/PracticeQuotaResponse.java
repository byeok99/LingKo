package com.lingko.lingko.api.quota.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 현재 평가 기회와 서버 기준 다음 자연 충전 시각을 HTTP 경계에 제공한다.
 *
 * 영속 엔티티를 직접 노출하지 않고 전송 계약을 독립적으로 유지하기 위해 전용 DTO를 선택했다.
 */
public record PracticeQuotaResponse(
        LocalDate date,
        int freeLimit,
        int freeUsed,
        int rewardedAvailable,
        int remainingPractices,
        OffsetDateTime nextRefillAt,
        OffsetDateTime serverTime
) {
}
