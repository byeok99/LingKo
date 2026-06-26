ALTER TABLE evaluation_log MODIFY COLUMN original_word VARCHAR(300) NOT NULL;
ALTER TABLE evaluation_log ADD COLUMN source VARCHAR(30) NOT NULL DEFAULT 'CUSTOM';
ALTER TABLE evaluation_log ADD COLUMN sentence_id BIGINT;
ALTER TABLE evaluation_log ADD COLUMN standard_pronunciation VARCHAR(300) NOT NULL DEFAULT '';
ALTER TABLE evaluation_log ADD COLUMN recognized_text VARCHAR(300);
ALTER TABLE evaluation_log ADD COLUMN accuracy_score DECIMAL(5, 2);
ALTER TABLE evaluation_log ADD COLUMN fluency_score DECIMAL(5, 2);
ALTER TABLE evaluation_log ADD COLUMN completeness_score DECIMAL(5, 2);
ALTER TABLE evaluation_log ADD COLUMN pronunciation_score DECIMAL(5, 2);
ALTER TABLE evaluation_log ADD COLUMN audio_url VARCHAR(500);

CREATE INDEX idx_evaluation_log_source_created
    ON evaluation_log (source, created_at);

CREATE INDEX idx_evaluation_log_sentence_created
    ON evaluation_log (sentence_id, created_at);

ALTER TABLE evaluation_syllable ADD COLUMN position_no INT NOT NULL DEFAULT 0;
ALTER TABLE evaluation_syllable ADD COLUMN feedback VARCHAR(500);
ALTER TABLE evaluation_syllable ADD COLUMN mouth_guide_url VARCHAR(500);
ALTER TABLE evaluation_syllable ADD COLUMN tongue_guide_url VARCHAR(500);

CREATE UNIQUE INDEX uk_evaluation_syllable_log_position
    ON evaluation_syllable (evaluation_log_idx, position_no);
