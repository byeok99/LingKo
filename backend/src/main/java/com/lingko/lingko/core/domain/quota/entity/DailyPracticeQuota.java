package com.lingko.lingko.core.domain.quota.entity;

import com.lingko.lingko.core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Instant;

/**
 * Daily Practice 할당량 상태와 단일 aggregate 내부 계산 규칙을 영속화한다.
 *
 * 동시 요청의 예약·확정·복구는 조건부 DB UPDATE가 원자성을 소유하고, 이 엔티티는 조회 결과 계산과
 * 테스트·관리 작업에서 사용하는 단일 transaction 상태 전이만 담당한다.
 */
@Entity
@Table(
        name = "daily_practice_quota",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_idx", "quota_date"})
        },
        indexes = {
                @Index(name = "idx_daily_practice_quota_date", columnList = "quota_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyPracticeQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_practice_quota_id")
    private Long dailyPracticeQuotaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;

    @Column(name = "quota_date", nullable = false)
    private LocalDate quotaDate;

    @Column(name = "free_limit", nullable = false)
    private int freeLimit;

    @Column(name = "free_used", nullable = false)
    private int freeUsed;

    @Column(name = "rewarded_available", nullable = false)
    private int rewardedAvailable;

    @Column(name = "free_reserved", nullable = false)
    @Builder.Default
    private int freeReserved = 0;

    @Column(name = "rewarded_reserved", nullable = false)
    @Builder.Default
    private int rewardedReserved = 0;

    /**
     * 다음 무료 1회가 자연 충전되는 서버 시각이며 null은 충전 대기가 없는 상태다.
     *
     * 최대치 보유나 예약 취소로 대기가 사라지면 null로 되돌려, 클라이언트가 남은 시간을
     * 계산할 수 있는 경우와 카운트다운을 감출 경우를 상태값 하나로 구분하게 한다.
     */
    @Column(name = "next_refill_at")
    private Instant nextRefillAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static DailyPracticeQuota create(User user, LocalDate quotaDate, int freeLimit) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        if (quotaDate == null) {
            throw new IllegalArgumentException("quotaDate must not be null");
        }
        if (freeLimit < 0) {
            throw new IllegalArgumentException("freeLimit must not be negative");
        }

        return DailyPracticeQuota.builder()
                .user(user)
                .quotaDate(quotaDate)
                .freeLimit(freeLimit)
                .freeUsed(0)
                .rewardedAvailable(0)
                .freeReserved(0)
                .rewardedReserved(0)
                .build();
    }

    public int remainingPractices() {
        // 손상된 과거 계수가 음수 할당량을 노출하지 않도록 무료 잔여량의 최솟값을 0으로 제한한다.
        return Math.max(0, freeLimit - freeUsed - freeReserved)
                + Math.max(0, rewardedAvailable - rewardedReserved);
    }

    public boolean hasRemainingPractice() {
        return remainingPractices() > 0;
    }

    /**
     * 서버 기준으로 완료된 1시간 구간만큼 무료 사용량을 되돌리고 최대치에서는 timer를 제거한다.
     * 광고 등 보상 횟수는 이 자연 충전 clock과 독립적으로 유지한다.
     */
    public void replenishAt(Instant now, Duration refillInterval) {
        if (now == null || refillInterval == null || refillInterval.isZero() || refillInterval.isNegative()) {
            throw new IllegalArgumentException("valid refill time is required");
        }
        if (freeUsed <= 0) {
            // 예약 중인 평가는 아직 확정 사용량이 아니므로 timer만 유지하고 선충전하지 않는다.
            if (freeReserved == 0) {
                nextRefillAt = null;
            }
            return;
        }
        if (nextRefillAt == null) {
            nextRefillAt = now.plus(refillInterval);
            return;
        }
        if (now.isBefore(nextRefillAt)) {
            return;
        }

        long elapsedIntervals = 1 + Duration.between(nextRefillAt, now).dividedBy(refillInterval);
        int replenished = (int) Math.min(freeUsed, elapsedIntervals);
        freeUsed -= replenished;
        nextRefillAt = freeUsed == 0
                ? null
                : nextRefillAt.plus(refillInterval.multipliedBy(replenished));
    }

    public void consumePractice() {
        // 획득한 보상 가치를 보존하기 위해 무료 제공량을 보상보다 먼저 사용한다.
        if (freeUsed < freeLimit) {
            freeUsed++;
            return;
        }
        if (rewardedAvailable > 0) {
            rewardedAvailable--;
            return;
        }

        throw new IllegalStateException("quota is exhausted");
    }

    public void useFreePractices(int count) {
        if (count < 0 || freeUsed + count > freeLimit) {
            throw new IllegalArgumentException("invalid free practice count");
        }

        freeUsed += count;
    }

    public void addRewardedPractices(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("reward count must not be negative");
        }

        rewardedAvailable += count;
    }
}
