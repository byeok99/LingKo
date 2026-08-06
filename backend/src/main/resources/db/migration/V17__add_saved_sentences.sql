-- 사용자가 다시 연습하려고 저장한 추천 문장을 기록한다.
-- 같은 문장을 두 번 저장할 수 없도록 사용자·문장 조합을 유일하게 둔다.
CREATE TABLE saved_sentence (
    saved_sentence_idx BIGINT NOT NULL AUTO_INCREMENT,
    user_idx BIGINT NOT NULL,
    sentence_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (saved_sentence_idx),
    CONSTRAINT fk_saved_sentence_user
        FOREIGN KEY (user_idx) REFERENCES users (user_idx)
);

CREATE UNIQUE INDEX uk_saved_sentence_user_sentence
    ON saved_sentence (user_idx, sentence_id);

-- 목록은 최근 저장 순으로 보여주므로 사용자별 정렬 조회를 인덱스로 받친다.
CREATE INDEX idx_saved_sentence_user_created
    ON saved_sentence (user_idx, created_at DESC);
