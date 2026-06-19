CREATE TABLE recommended_sentences (
    sentence_id BIGINT NOT NULL AUTO_INCREMENT,
    original_text VARCHAR(120) NOT NULL,
    standard_pronunciation VARCHAR(120) NOT NULL,
    translation VARCHAR(255) NOT NULL,
    level_label VARCHAR(50) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    category_label VARCHAR(50) NOT NULL,
    learning_point VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (sentence_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_recommended_sentences_active_sort
    ON recommended_sentences (active, sort_order, sentence_id);

CREATE INDEX idx_recommended_sentences_category_active_sort
    ON recommended_sentences (category_code, active, sort_order, sentence_id);

INSERT INTO recommended_sentences
    (original_text, standard_pronunciation, translation, level_label, category_code, category_label, learning_point, active, sort_order)
VALUES
    ('맛있겠다.', '마싯게따.', 'It looks delicious.', 'Beginner 2', 'FOOD', 'Food', 'Final consonant linking and tense sound', TRUE, 1),
    ('김치찌개 하나 주세요.', '김치찌개 하나 주세요.', 'Please give me one kimchi stew.', 'Beginner 2', 'FOOD', 'Food', 'Tense consonants in food ordering', TRUE, 2),
    ('물 한 잔 주세요.', '물 한 잔 주세요.', 'Please give me a glass of water.', 'Beginner 1', 'FOOD', 'Food', 'Final consonant clarity in short requests', TRUE, 3),
    ('커피가 뜨거워요.', '커피가 뜨거워요.', 'The coffee is hot.', 'Beginner 2', 'FOOD', 'Food', 'Aspirated consonant and rounded vowel practice', TRUE, 4),
    ('이거 포장해 주세요.', '이거 포장해 주세요.', 'Please pack this to go.', 'Beginner 2', 'FOOD', 'Food', 'Polite request rhythm', TRUE, 5),
    ('천천히 말씀해 주세요.', '천처니 말쓰매 주세요.', 'Please speak slowly.', 'Beginner 2', 'DAILY', 'Daily', 'Aspirated consonants and linking', TRUE, 6),
    ('오늘 날씨가 좋아요.', '오늘 날씨가 조아요.', 'The weather is nice today.', 'Beginner 1', 'DAILY', 'Daily', 'Natural sentence rhythm', TRUE, 7),
    ('지하철역이 어디예요?', '지하철려기 어디예요?', 'Where is the subway station?', 'Beginner 2', 'TRAVEL', 'Travel', 'Linking across compound words', TRUE, 8),
    ('왼쪽으로 가세요.', '왼쪼그로 가세요.', 'Please go to the left.', 'Beginner 2', 'TRAVEL', 'Travel', 'Tense consonant after final consonant', TRUE, 9),
    ('사진 찍어도 돼요?', '사진 찌거도 돼요?', 'May I take a photo?', 'Beginner 2', 'TRAVEL', 'Travel', 'Tense consonant in connected speech', TRUE, 10),
    ('한국어를 배우고 있어요.', '한구거를 배우고 이써요.', 'I am learning Korean.', 'Beginner 1', 'STUDY', 'Study', 'Linking across syllables', TRUE, 11),
    ('다시 한번 말해 주세요.', '다시 한번 말해 주세요.', 'Please say it one more time.', 'Beginner 1', 'STUDY', 'Study', 'Polite classroom request', TRUE, 12),
    ('발음이 어려워요.', '바르미 어려워요.', 'The pronunciation is difficult.', 'Beginner 2', 'STUDY', 'Study', 'Final consonant linking before a vowel', TRUE, 13),
    ('숙제는 다 했어요.', '숙쩨는 다 해써요.', 'I finished all the homework.', 'Beginner 2', 'STUDY', 'Study', 'Tense sound after final consonant', TRUE, 14),
    ('친구를 만났어요.', '친구를 만나써요.', 'I met a friend.', 'Beginner 1', 'DAILY', 'Daily', 'Past tense ending with tense sound', TRUE, 15),
    ('회사에 늦었어요.', '회사에 느저써요.', 'I was late to work.', 'Beginner 2', 'WORK', 'Work', 'Final consonant before vowel ending', TRUE, 16),
    ('회의가 몇 시예요?', '회의가 멷 씨예요?', 'What time is the meeting?', 'Beginner 2', 'WORK', 'Work', 'Question rhythm and tense consonants', TRUE, 17),
    ('메일 확인했어요.', '메일 화긴해써요.', 'I checked the email.', 'Beginner 2', 'WORK', 'Work', 'H consonant and past tense ending', TRUE, 18),
    ('병원에 가야 해요.', '병워네 가야 해요.', 'I need to go to the hospital.', 'Beginner 2', 'HEALTH', 'Health', 'Nasal consonant linking', TRUE, 19),
    ('약을 먹어야 돼요.', '야글 머거야 돼요.', 'I need to take medicine.', 'Beginner 2', 'HEALTH', 'Health', 'Final consonant linking in common phrase', TRUE, 20),
    ('운동을 시작했어요.', '운동을 시자캐써요.', 'I started exercising.', 'Beginner 2', 'HEALTH', 'Health', 'Aspirated consonant shift with h', TRUE, 21);
