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

/**
 * Daily Practice 할당량 상태를 영속화하고 불변 조건를 지키는 상태 전이를 소유한다.
 *
 * 어떤 서비스가 호출해도 동일한 규칙이 유지되어야 하는 동작이므로 데이터를 가진 엔티티에 배치했다.
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
                .build();
    }

    public int remainingPractices() {
        // 손상된 과거 계수가 음수 할당량을 노출하지 않도록 무료 잔여량의 최솟값을 0으로 제한한다.
        return Math.max(0, freeLimit - freeUsed) + rewardedAvailable;
    }

    public boolean hasRemainingPractice() {
        return remainingPractices() > 0;
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
