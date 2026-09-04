package com.lingko.lingko.core.domain.quota.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** 앱 광고 한 건과 Google signed callback을 연결하는 1회성 서버 세션이다. */
@Entity
@Table(
        name = "ad_reward_sessions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "session_token_hash"),
                @UniqueConstraint(columnNames = "transaction_id")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdRewardSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ad_reward_session_id")
    private Long id;

    @Column(name = "user_idx", nullable = false)
    private Long userId;

    /** 원본 token은 앱에만 전달하고 DB에는 SHA-256만 저장한다. */
    @Column(name = "session_token_hash", nullable = false, length = 64)
    private String sessionTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AdRewardSessionStatus status;

    /** Google이 서명한 전역 고유 지급 식별자다. */
    @Column(name = "transaction_id", length = 80)
    private String transactionId;

    @Column(name = "credited")
    private Boolean credited;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static AdRewardSession pending(Long userId, String tokenHash, Instant expiresAt) {
        if (userId == null || tokenHash == null || tokenHash.length() != 64 || expiresAt == null) {
            throw new IllegalArgumentException("ad reward session identity must be complete");
        }
        return AdRewardSession.builder()
                .userId(userId)
                .sessionTokenHash(tokenHash)
                .status(AdRewardSessionStatus.PENDING)
                .expiresAt(expiresAt)
                .build();
    }

    public boolean isExpiredAt(Instant now) {
        return status == AdRewardSessionStatus.PENDING && !now.isBefore(expiresAt);
    }

    public void expire() {
        if (status == AdRewardSessionStatus.PENDING) {
            status = AdRewardSessionStatus.EXPIRED;
        }
    }

    public void complete(String verifiedTransactionId, boolean wasCredited, Instant now) {
        if (status != AdRewardSessionStatus.PENDING) {
            return;
        }
        transactionId = verifiedTransactionId;
        credited = wasCredited;
        completedAt = now;
        status = AdRewardSessionStatus.COMPLETED;
    }
}
