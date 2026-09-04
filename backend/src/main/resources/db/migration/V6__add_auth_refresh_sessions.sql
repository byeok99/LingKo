CREATE TABLE auth_refresh_sessions (
    session_id CHAR(36) NOT NULL,
    user_idx BIGINT NOT NULL,
    current_token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (session_id),
    CONSTRAINT uk_auth_refresh_sessions_token_hash UNIQUE (current_token_hash),
    CONSTRAINT fk_auth_refresh_sessions_user
        FOREIGN KEY (user_idx) REFERENCES users (user_idx) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_auth_refresh_sessions_user
    ON auth_refresh_sessions (user_idx, revoked_at);

CREATE INDEX idx_auth_refresh_sessions_expiry
    ON auth_refresh_sessions (expires_at);
