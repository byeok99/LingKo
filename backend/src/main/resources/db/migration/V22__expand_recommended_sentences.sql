-- 기존 21개 문장의 ID·표시 순서를 보존하면서 초급 생활 문장을 6개 주제별 8개로 확장한다.
-- 총 48개는 현재 앱의 limit=50 안에 모두 포함된다. 표준 발음은 저장하지 않고 조회 시 현재 규칙으로 계산한다.
-- ID는 자동 발급해 운영에서 먼저 추가된 콘텐츠와의 기본 키 충돌을 피한다.
INSERT INTO recommended_sentences
    (original_text, translation, level_label, category_code, category_label, learning_point, active, sort_order)
VALUES
    ('덜 맵게 해 주세요.', 'Please make it less spicy.', 'Beginner 2', 'FOOD', 'Food', 'Final consonants in a polite food request', TRUE, 22),
    ('같이 먹어요.', 'Let us eat together.', 'Beginner 1', 'FOOD', 'Food', 'Linking in a short invitation', TRUE, 23),
    ('계산해 주세요.', 'The bill, please.', 'Beginner 1', 'FOOD', 'Food', 'Clear syllable rhythm in a polite request', TRUE, 24),
    ('만나서 반가워요.', 'Nice to meet you.', 'Beginner 1', 'DAILY', 'Daily', 'Natural rhythm in a greeting', TRUE, 25),
    ('이름이 뭐예요?', 'What is your name?', 'Beginner 1', 'DAILY', 'Daily', 'Final consonant linking and question rhythm', TRUE, 26),
    ('오늘은 집에서 쉬어요.', 'I am resting at home today.', 'Beginner 1', 'DAILY', 'Daily', 'Linking before vowel endings', TRUE, 27),
    ('내일 몇 시에 만나요?', 'What time shall we meet tomorrow?', 'Beginner 2', 'DAILY', 'Daily', 'Time expressions and question rhythm', TRUE, 28),
    ('비가 와서 우산을 가져왔어요.', 'I brought an umbrella because it is raining.', 'Beginner 2', 'DAILY', 'Daily', 'Connected speech in a longer sentence', TRUE, 29),
    ('여기에서 내려 주세요.', 'Please let me off here.', 'Beginner 1', 'TRAVEL', 'Travel', 'Polite request rhythm while traveling', TRUE, 30),
    ('화장실이 어디에 있어요?', 'Where is the restroom?', 'Beginner 1', 'TRAVEL', 'Travel', 'Final consonant linking in a location question', TRUE, 31),
    ('표 두 장 주세요.', 'Two tickets, please.', 'Beginner 1', 'TRAVEL', 'Travel', 'Aspirated consonants and counting rhythm', TRUE, 32),
    ('공항까지 얼마나 걸려요?', 'How long does it take to get to the airport?', 'Beginner 2', 'TRAVEL', 'Travel', 'Nasal and liquid consonants in connected speech', TRUE, 33),
    ('예약한 방이 있어요.', 'I have a room reservation.', 'Beginner 2', 'TRAVEL', 'Travel', 'Final consonant linking in a hotel phrase', TRUE, 34),
    ('이 단어는 무슨 뜻이에요?', 'What does this word mean?', 'Beginner 2', 'STUDY', 'Study', 'Final consonant linking in a classroom question', TRUE, 35),
    ('칠판을 봐 주세요.', 'Please look at the board.', 'Beginner 1', 'STUDY', 'Study', 'Aspirated consonants and vowel transitions', TRUE, 36),
    ('책을 소리 내어 읽어요.', 'I read the book aloud.', 'Beginner 2', 'STUDY', 'Study', 'Final consonant linking while reading aloud', TRUE, 37),
    ('매일 조금씩 연습해요.', 'I practice a little every day.', 'Beginner 2', 'STUDY', 'Study', 'Tense and aspirated consonants in connected speech', TRUE, 38),
    ('지금 통화할 수 있어요?', 'Can you talk on the phone now?', 'Beginner 2', 'WORK', 'Work', 'Question rhythm in a phone conversation', TRUE, 39),
    ('자료를 보내 주세요.', 'Please send me the materials.', 'Beginner 1', 'WORK', 'Work', 'Liquid consonants in a polite request', TRUE, 40),
    ('잠시만 기다려 주세요.', 'Please wait a moment.', 'Beginner 1', 'WORK', 'Work', 'Natural pauses in a polite request', TRUE, 41),
    ('회의는 오후에 시작해요.', 'The meeting starts in the afternoon.', 'Beginner 2', 'WORK', 'Work', 'Vowel transitions and aspirated consonants', TRUE, 42),
    ('도와주셔서 감사합니다.', 'Thank you for helping me.', 'Beginner 2', 'WORK', 'Work', 'Nasal consonants and formal ending rhythm', TRUE, 43),
    ('머리가 아파요.', 'I have a headache.', 'Beginner 1', 'HEALTH', 'Health', 'Clear vowels when describing symptoms', TRUE, 44),
    ('목이 아프고 기침이 나요.', 'I have a sore throat and a cough.', 'Beginner 2', 'HEALTH', 'Health', 'Final consonant linking and aspirated consonants', TRUE, 45),
    ('어제부터 열이 나요.', 'I have had a fever since yesterday.', 'Beginner 2', 'HEALTH', 'Health', 'Linking and rhythm when describing symptoms', TRUE, 46),
    ('식사 후에 드세요.', 'Please take it after a meal.', 'Beginner 2', 'HEALTH', 'Health', 'Final consonants and polite instruction rhythm', TRUE, 47),
    ('오늘은 일찍 잘 거예요.', 'I am going to bed early today.', 'Beginner 2', 'HEALTH', 'Health', 'Tense consonants and future ending rhythm', TRUE, 48);
