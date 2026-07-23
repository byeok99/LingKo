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

    public static RefreshTokenSession create(
            String sessionId,
            User user,
            String currentTokenHash,
            Instant expiresAt
    ) {
        return new RefreshTokenSession(sessionId, user, currentTokenHash, expiresAt);
    }

    public void rotate(String nextTokenHash) {
        currentTokenHash = nextTokenHash;
    }

    public void revoke(Instant revokedAt) {
        if (this.revokedAt == null) {
            this.revokedAt = revokedAt;
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
