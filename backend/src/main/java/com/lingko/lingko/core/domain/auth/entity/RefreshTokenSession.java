package com.lingko.lingko.core.domain.auth.entity;

import com.lingko.lingko.core.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * 일반적으로 앱 설치 또는 기기 하나에 대응하는 독립 폐기 가능한 로그인 세션을 나타낸다.
 *
 * <p>현재 갱신 토큰 해시만 보관하며 회전 시 해당 해시를 교체한다.
 * 로그아웃이나 재사용 탐지는 되돌릴 수 없는 폐기 시각을 기록한다.</p>
 */
@Entity
@Table(name = "auth_refresh_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenSession {

    @Id
    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;

    @Column(name = "current_token_hash", nullable = false, unique = true, length = 64)
    private String currentTokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private RefreshTokenSession(
            String sessionId,
            User user,
            String currentTokenHash,
            Instant expiresAt
    ) {
        this.sessionId = sessionId;
        this.user = user;
        this.currentTokenHash = currentTokenHash;
        this.expiresAt = expiresAt;
    }

    /**
     * 새로 발급한 토큰 계열의 초기 영속 상태를 생성한다.
     */
    public static RefreshTokenSession create(
            String sessionId,
            User user,
            String currentTokenHash,
            Instant expiresAt
    ) {
        return new RefreshTokenSession(sessionId, user, currentTokenHash, expiresAt);
    }

    /**
     * 세션을 새로 발급한 갱신 토큰 fingerprint로 전진시킨다.
     */
    public void rotate(String nextTokenHash) {
        currentTokenHash = nextTokenHash;
    }

    /**
     * 최초 폐기 시각을 보존하면서 세션을 최종 폐기 상태로 표시한다.
     */
    public void revoke(Instant revokedAt) {
        if (this.revokedAt == null) {
            this.revokedAt = revokedAt;
        }
    }

    /**
     * 이 토큰 계열이 최종 폐기됐는지 반환한다.
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * 토큰 회전으로 연장되지 않는 세션 절대 수명을 판정한다.
     */
    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
