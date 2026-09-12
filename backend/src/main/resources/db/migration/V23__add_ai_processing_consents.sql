-- 약관 확인과 별도로 AI 전송 허용·철회를 기록한다. 기존 사용자에게 허용을 자동 부여하지 않는다.
CREATE TABLE ai_processing_consents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_idx BIGINT NOT NULL,
    notice_version VARCHAR(32) NOT NULL,
    granted BOOLEAN NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_ai_consent_user_id (user_idx, id),
    CONSTRAINT fk_ai_processing_consent_user FOREIGN KEY (user_idx)
        REFERENCES users (user_idx) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- NULL인 기존 작업은 전송 근거가 없으므로 Worker가 실패·쿼터 복구 처리한다.
ALTER TABLE evaluation_jobs ADD COLUMN ai_consent_id BIGINT NULL;
