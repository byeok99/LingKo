package com.lingko.lingko.core.domain.quota.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * 광고 SDK가 한 번 전달한 reward event를 사용자별로 기록해 재전송의 중복 지급을 막는다.
 *
 * <p>광고 보상 수량은 저장하지 않는다. 서버 정책은 event 하나당 항상 1회이며,
 * 클라이언트가 임의 수량을 전달해 평가 기회를 늘릴 수 없도록 하기 위한 선택이다.</p>
 */
@Entity
@Table(
        name = "ad_reward_receipts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_idx", "reward_event_id"})
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdRewardReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ad_reward_receipt_id")
    private Long id;

    /** 활성 Bearer session에서 확인한 사용자 식별자다. body의 사용자 값은 받지 않는다. */
    @Column(name = "user_idx", nullable = false)
    private Long userId;

    /** 앱이 광고 표시 한 건마다 만든 idempotency 식별자다. */
    @Column(name = "reward_event_id", nullable = false, length = 80)
    private String rewardEventId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static AdRewardReceipt create(Long userId, String rewardEventId) {
        if (userId == null || rewardEventId == null || rewardEventId.isBlank()) {
            throw new IllegalArgumentException("reward receipt identity must be complete");
        }
        return AdRewardReceipt.builder()
                .userId(userId)
                .rewardEventId(rewardEventId)
                .build();
    }
}
