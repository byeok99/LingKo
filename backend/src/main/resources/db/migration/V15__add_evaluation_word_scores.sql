-- 공급자가 신뢰할 수 있게 제공한 단어 점수는 한 번만 저장하고 음절은 guide-only로 연결한다.
ALTER TABLE evaluation_syllable
    MODIFY COLUMN score INT NULL;

ALTER TABLE evaluation_syllable
    ADD COLUMN word_position INT NULL AFTER position_no;

CREATE TABLE evaluation_word (
    evaluation_word_idx BIGINT NOT NULL AUTO_INCREMENT,
    evaluation_log_idx BIGINT NOT NULL,
    position_no INT NOT NULL,
    word_text VARCHAR(100) NOT NULL,
    score INT NULL,
    PRIMARY KEY (evaluation_word_idx),
    CONSTRAINT fk_evaluation_word_log
        FOREIGN KEY (evaluation_log_idx) REFERENCES evaluation_log (evaluation_log_idx)
);

CREATE UNIQUE INDEX uk_evaluation_word_log_position
    ON evaluation_word (evaluation_log_idx, position_no);

CREATE INDEX idx_evaluation_word_log
    ON evaluation_word (evaluation_log_idx);
