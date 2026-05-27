import '../models/practice_sentence.dart';

// 백엔드가 붙기 전 디자인을 확인하기 위한 더미 데이터입니다.
// MVP에서는 추천 문장 목록 API 응답으로 대체될 예정입니다.
const mockSentences = [
  PracticeSentence(
    text: '맛있겠다.',
    pronunciation: '마싯게따.',
    translation: 'It looks delicious.',
    level: 'Beginner 2',
    category: 'Food',
    point: 'Final consonant linking and tense sound',
    score: 82,
    characters: [
      CharacterResult(
        character: '마',
        score: 94,
        note: 'Stable vowel shape',
        kind: 'Mouth',
      ),
      CharacterResult(
        character: '싯',
        score: 68,
        note: 'Keep the tongue closer for the sibilant sound',
        kind: 'Tongue',
      ),
      CharacterResult(
        character: '게',
        score: 86,
        note: 'Good transition into the next syllable',
        kind: 'Mouth',
      ),
      CharacterResult(
        character: '따',
        score: 72,
        note: 'Make the tense consonant shorter and stronger',
        kind: 'Tongue',
      ),
    ],
  ),
  PracticeSentence(
    text: '천천히 말씀해 주세요.',
    pronunciation: '천처니 말쓰매 주세요.',
    translation: 'Please speak slowly.',
    level: 'Beginner 2',
    category: 'Daily',
    point: 'Aspirated consonants and linking',
    score: 78,
    characters: [
      CharacterResult(
        character: '천',
        score: 81,
        note: 'Air release is clear',
        kind: 'Mouth',
      ),
      CharacterResult(
        character: '처',
        score: 77,
        note: 'Keep the jaw relaxed',
        kind: 'Mouth',
      ),
      CharacterResult(
        character: '니',
        score: 84,
        note: 'Good final consonant transfer',
        kind: 'Tongue',
      ),
      CharacterResult(
        character: '쓰',
        score: 62,
        note: 'Tense consonant needs more pressure',
        kind: 'Tongue',
      ),
    ],
  ),
  PracticeSentence(
    text: '한국어를 배우고 있어요.',
    pronunciation: '한구거를 배우고 이써요.',
    translation: 'I am learning Korean.',
    level: 'Beginner 1',
    category: 'Study',
    point: 'Linking across syllables',
    score: 88,
    characters: [
      CharacterResult(
        character: '한',
        score: 89,
        note: 'Good final consonant',
        kind: 'Tongue',
      ),
      CharacterResult(
        character: '구',
        score: 92,
        note: 'Stable rounded vowel',
        kind: 'Mouth',
      ),
      CharacterResult(
        character: '거',
        score: 80,
        note: 'Soften the transition',
        kind: 'Tongue',
      ),
      CharacterResult(
        character: '써',
        score: 73,
        note: 'Tense consonant can be clearer',
        kind: 'Tongue',
      ),
    ],
  ),
];
