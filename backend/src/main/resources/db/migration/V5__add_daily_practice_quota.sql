CREATE TABLE daily_practice_quota (
    daily_practice_quota_id BIGINT NOT NULL AUTO_INCREMENT,
    user_idx BIGINT NOT NULL,
    quota_date DATE NOT NULL,
    free_limit INT NOT NULL DEFAULT 5,
    free_used INT NOT NULL DEFAULT 0,
    rewarded_available INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (daily_practice_quota_id),
    CONSTRAINT uk_daily_practice_quota_user_date UNIQUE (user_idx, quota_date),
    CONSTRAINT fk_daily_practice_quota_user
        FOREIGN KEY (user_idx) REFERENCES users (user_idx)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_daily_practice_quota_date
    ON daily_practice_quota (quota_date);
