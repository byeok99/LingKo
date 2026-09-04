CREATE TABLE ad_reward_receipts (
    ad_reward_receipt_id BIGINT NOT NULL AUTO_INCREMENT,
    user_idx BIGINT NOT NULL,
    reward_event_id VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ad_reward_receipt_id),
    CONSTRAINT uq_ad_reward_receipts_user_event UNIQUE (user_idx, reward_event_id),
    CONSTRAINT fk_ad_reward_receipts_user
        FOREIGN KEY (user_idx) REFERENCES users (user_idx) ON DELETE CASCADE
);

CREATE INDEX idx_ad_reward_receipts_user_created
    ON ad_reward_receipts (user_idx, created_at);
