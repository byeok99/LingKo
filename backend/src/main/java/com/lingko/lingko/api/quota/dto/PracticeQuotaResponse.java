package com.lingko.lingko.api.quota.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 현재 평가 기회와 서버 기준 다음 자연 충전 시각을 HTTP 경계에 제공한다.
 *
 * 영속 엔티티를 직접 노출하지 않고 전송 계약을 독립적으로 유지하기 위해 전용 DTO를 선택했다.
 *
 * {@code nextRefillAt}은 다음 자연 충전 시각이며 null이면 충전 대기가 없다는 뜻이다.
 * {@code serverTime}을 함께 보내는 이유는 클라이언트가 기기 시계 대신 두 서버 시각의 차이로
 * 남은 시간을 계산해, 기기 시간대·시계 오차가 카운트다운을 어긋나게 하지 않도록 하기 위해서다.
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
