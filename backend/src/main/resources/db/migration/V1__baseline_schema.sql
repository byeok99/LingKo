CREATE TABLE users (
    user_idx BIGINT NOT NULL AUTO_INCREMENT,
    social_id VARCHAR(255) NOT NULL,
    social_type VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    name VARCHAR(100),
    profile_image_url VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    last_login_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_idx),
    CONSTRAINT uk_users_social UNIQUE (social_id, social_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE syllables (
    syllable_char VARCHAR(10) NOT NULL,
    mouth_url VARCHAR(500),
    tongue_url VARCHAR(500),
    PRIMARY KEY (syllable_char)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE evaluation_log (
    evaluation_log_idx BIGINT NOT NULL AUTO_INCREMENT,
    user_idx BIGINT NOT NULL,
    original_word VARCHAR(50) NOT NULL,
    score INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (evaluation_log_idx),
    CONSTRAINT fk_evaluation_log_user
        FOREIGN KEY (user_idx) REFERENCES users (user_idx)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_user_created ON evaluation_log (user_idx, created_at);

CREATE TABLE evaluation_syllable (
    evaluation_syllables_idx BIGINT NOT NULL AUTO_INCREMENT,
    evaluation_log_idx BIGINT NOT NULL,
    syllable_char VARCHAR(10) NOT NULL,
    score INT NOT NULL,
    PRIMARY KEY (evaluation_syllables_idx),
    CONSTRAINT fk_evaluation_syllable_log
        FOREIGN KEY (evaluation_log_idx) REFERENCES evaluation_log (evaluation_log_idx),
    CONSTRAINT fk_evaluation_syllable_syllable
        FOREIGN KEY (syllable_char) REFERENCES syllables (syllable_char)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_evaluation_syllable_log ON evaluation_syllable (evaluation_log_idx);
CREATE INDEX idx_evaluation_syllable_char ON evaluation_syllable (syllable_char);
