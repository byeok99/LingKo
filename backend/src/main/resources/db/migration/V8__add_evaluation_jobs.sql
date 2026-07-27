CREATE TABLE evaluation_jobs (
    job_id CHAR(36) NOT NULL,
    user_idx BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    audio_object_key VARCHAR(500) NOT NULL,
    source VARCHAR(30) NOT NULL,
    sentence_id BIGINT,
    original_text VARCHAR(300) NOT NULL,
    standard_pronunciation VARCHAR(300) NOT NULL,
    quota_date DATE NOT NULL,
    quota_source VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    lease_expires_at DATETIME(6),
    result_payload LONGTEXT,
    error_code VARCHAR(80),
    completed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (job_id),
    CONSTRAINT uk_evaluation_jobs_user_idempotency UNIQUE (user_idx, idempotency_key),
    CONSTRAINT uk_evaluation_jobs_audio_object UNIQUE (audio_object_key),
    CONSTRAINT fk_evaluation_jobs_user
        FOREIGN KEY (user_idx) REFERENCES users (user_idx) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_evaluation_jobs_claim
    ON evaluation_jobs (status, next_attempt_at, lease_expires_at, created_at);
