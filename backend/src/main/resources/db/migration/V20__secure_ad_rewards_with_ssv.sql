ALTER TABLE ad_reward_receipts
    ADD COLUMN provider_transaction_id VARCHAR(80) NULL;

ALTER TABLE ad_reward_receipts
    ADD CONSTRAINT uq_ad_reward_receipts_provider_transaction UNIQUE (provider_transaction_id);

CREATE TABLE ad_reward_sessions (
    ad_reward_session_id BIGINT NOT NULL AUTO_INCREMENT,
    user_idx BIGINT NOT NULL,
    session_token_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    transaction_id VARCHAR(80) NULL,
    credited BOOLEAN NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at TIMESTAMP(6) NULL,
    PRIMARY KEY (ad_reward_session_id),
    CONSTRAINT uq_ad_reward_sessions_token UNIQUE (session_token_hash),
    CONSTRAINT uq_ad_reward_sessions_transaction UNIQUE (transaction_id),
    CONSTRAINT fk_ad_reward_sessions_user
        FOREIGN KEY (user_idx) REFERENCES users (user_idx) ON DELETE CASCADE
);

CREATE INDEX idx_ad_reward_sessions_user_status
    ON ad_reward_sessions (user_idx, status);
