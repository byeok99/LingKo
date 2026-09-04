-- 사용자별 법무 문서 버전 동의 사실을 감사 가능한 이력으로 보존한다.
CREATE TABLE legal_consents (
    legal_consent_idx BIGINT NOT NULL AUTO_INCREMENT,
    user_idx BIGINT NOT NULL,
    document_version VARCHAR(32) NOT NULL,
    terms_agreed BOOLEAN NOT NULL,
    privacy_acknowledged BOOLEAN NOT NULL,
    marketing_opt_in BOOLEAN NOT NULL,
    client_agreed_at DATETIME(6) NOT NULL,
    recorded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (legal_consent_idx),
    CONSTRAINT uk_legal_consents_user_version UNIQUE (user_idx, document_version),
    CONSTRAINT fk_legal_consents_user
        FOREIGN KEY (user_idx) REFERENCES users (user_idx) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 사용자 동의 이력을 최신 기록 순으로 조회할 수 있게 한다.
CREATE INDEX idx_legal_consents_user_recorded
    ON legal_consents (user_idx, recorded_at DESC);
