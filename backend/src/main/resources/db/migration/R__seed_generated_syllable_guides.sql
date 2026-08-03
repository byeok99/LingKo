-- 출시 전 생성 완료된 MP4를 누적한다. repeatable migration이므로 행 추가 시 checksum 변경으로 다시 적용된다.
INSERT INTO syllables (syllable_char, mouth_url, tongue_url)
VALUES
    ('바', 'https://lingko.s3.ap-northeast-2.amazonaws.com/videos/mouth/mouth_591564f4060325ef713fd880.mp4', NULL),
    ('각', NULL, 'https://lingko.s3.ap-northeast-2.amazonaws.com/videos/tongue/tongue_34b88281955f9dde97f58ad4.mp4')
ON DUPLICATE KEY UPDATE
    mouth_url = CASE
        WHEN VALUES(mouth_url) IS NULL THEN mouth_url
        WHEN mouth_url IS NULL OR mouth_url NOT LIKE '%.mp4%' THEN VALUES(mouth_url)
        ELSE mouth_url
    END,
    tongue_url = CASE
        WHEN VALUES(tongue_url) IS NULL THEN tongue_url
        WHEN tongue_url IS NULL OR tongue_url NOT LIKE '%.mp4%' THEN VALUES(tongue_url)
        ELSE tongue_url
    END;
