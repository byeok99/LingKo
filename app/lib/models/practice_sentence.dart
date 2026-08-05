// 파일 의도: practice sentence의 앱 내부 데이터 의미와 API 매핑을 정의한다.
// 선택 이유: 동적 JSON을 형식이 지정된 model로 변환해 잘못된 응답을 UI 경계 전에 차단한다.

import '../utils/practice_sentence_normalizer.dart';
import 'score_status.dart';

// 화면에 표시할 학습 문장 하나를 표현하는 임시 데이터 모델입니다.
// 나중에 백엔드 API가 붙으면 이 값들은 서버 응답 DTO와 매핑됩니다.
/// Practice Sentence 값의 의미와 불변 데이터 구조를 나타낸다.
/// UI가 Map key나 nullable JSON 세부사항을 직접 다루지 않도록 형식이 지정된 model을 선택했다.
class PracticeSentence {
  const PracticeSentence({
    this.sentenceId,
    this.source = 'CUSTOM',
    required this.text,
    required this.pronunciation,
    this.romanizedPronunciation = '',
    required this.translation,
    required this.level,
    required this.category,
    required this.point,
    required this.score,
    required this.characters,
  });

  factory PracticeSentence.fromPrepareResponse(Map<String, Object?> json) {
    final sentence = json['sentence'];
    if (sentence is! Map<String, Object?>) {
      throw const FormatException('Missing prepared sentence');
    }

    final charactersJson = sentence['characters'];
    final characters =
        charactersJson is List
            ? charactersJson
                .whereType<Map<String, Object?>>()
                .map(CharacterResult.fromGuideJson)
                .toList()
            : <CharacterResult>[];

    return PracticeSentence(
      sentenceId: _intOrNullValue(sentence['sentenceId']),
      source: _stringValue(sentence['source'], fallback: 'CUSTOM'),
      text: normalizePracticeSentenceText(
        _stringValue(sentence['originalText']),
      ),
      pronunciation: normalizePracticeSentenceText(
        _stringValue(sentence['standardPronunciation']),
      ),
      romanizedPronunciation:
          _stringValue(sentence['romanizedPronunciation']).trim(),
      translation: _stringValue(sentence['translation']),
      level: _stringValue(sentence['source'], fallback: 'Custom'),
      category: _stringValue(sentence['categoryLabel']),
      point: _stringValue(sentence['learningPoint']),
      score: _intValue(sentence['initialScore']),
      characters: characters,
    );
  }

  factory PracticeSentence.fromSentenceJson(Map<String, Object?> json) {
    final charactersJson = json['characters'];
    final characters =
        charactersJson is List
            ? charactersJson
                .whereType<Map<String, Object?>>()
                .map(CharacterResult.fromGuideJson)
                .toList()
            : <CharacterResult>[];

    return PracticeSentence(
      sentenceId: _intOrNullValue(json['sentenceId']),
      source: _stringValue(json['source'], fallback: 'RECOMMENDED'),
      text: normalizePracticeSentenceText(_stringValue(json['originalText'])),
      pronunciation: normalizePracticeSentenceText(
        _stringValue(json['standardPronunciation']),
      ),
      romanizedPronunciation:
          _stringValue(json['romanizedPronunciation']).trim(),
      translation: _stringValue(json['translation']),
      level: _stringValue(json['source'], fallback: 'RECOMMENDED'),
      category: _stringValue(json['categoryLabel']),
      point: _stringValue(json['learningPoint']),
      score: _intValue(json['initialScore']),
      characters: characters,
    );
  }

  // 사용자가 직접 입력한 문장을 임시 연습 데이터로 바꿉니다.
  // 실제 구현에서는 백엔드가 표준 발음, 번역, 글자별 가이드를 계산해 내려줍니다.
  factory PracticeSentence.custom(String text) {
    final normalized = normalizePracticeSentenceText(text);

    return PracticeSentence(
      source: 'CUSTOM',
      text: normalized,
      pronunciation: 'Custom sentence',
      translation: 'Practice with your own sentence.',
      level: 'Custom',
      category: 'Free practice',
      point: 'Record your voice to receive pronunciation feedback.',
      score: 0,
      characters: _buildCustomCharacters(normalized),
    );
  }

  /// 추천·서버·기록 등 어떤 출처의 문장도 Practice 상태에 들어오기 전에 같은 규칙으로 정규화한다.
  PracticeSentence normalizedForPractice() {
    final normalizedText = normalizePracticeSentenceText(text);
    final normalizedPronunciation = normalizePracticeSentenceText(
      pronunciation,
    );

    return PracticeSentence(
      sentenceId: sentenceId,
      source: source,
      text: normalizedText,
      pronunciation: normalizedPronunciation,
      romanizedPronunciation: romanizedPronunciation.trim(),
      translation: translation,
      level: level,
      category: category,
      point: point,
      score: score,
      characters: characters
          .where(
            (character) =>
                normalizePracticeSentenceText(character.character).isNotEmpty,
          )
          .toList(growable: false),
    );
  }

  final int? sentenceId;
  final String source;
  final String text;
  final String pronunciation;

  /// 서버가 표준 발음에서 파생한 음절 단위 학습자용 로마자 읽기 가이드다.
  final String romanizedPronunciation;
  final String translation;
  final String level;
  final String category;
  final String point;
  final int score;
  final List<CharacterResult> characters;
}

List<CharacterResult> _buildCustomCharacters(String text) {
  final visibleCharacters =
      text.runes
          .map(String.fromCharCode)
          .where((character) => character.trim().isNotEmpty)
          .take(8)
          .toList();

  if (visibleCharacters.isEmpty) {
    return const [];
  }

  return [
    for (final character in visibleCharacters)
      CharacterResult(
        character: character,
        score: 0,
        note: 'Guide will be generated after pronunciation analysis.',
        kind: 'Custom',
      ),
  ];
}

// 평가 결과에서 글자별 점수와 피드백을 보여주기 위한 모델입니다.
/// Character Result 값의 의미와 불변 데이터 구조를 나타낸다.
/// UI가 Map key나 nullable JSON 세부사항을 직접 다루지 않도록 형식이 지정된 model을 선택했다.
class CharacterResult {
  const CharacterResult({
    required this.character,
    required this.score,
    required this.note,
    required this.kind,
    this.mouthGuideUrl,
    this.tongueGuideUrl,
    this.guideStatus,
    // 기본값을 unavailable로 두어, scoreStatus를 지정하지 않고 만든 값이
    // 신뢰할 수 없는 점수를 신뢰 가능한 것처럼 노출하는 fail-open을 막는다.
    this.scoreStatus = ScoreStatus.unavailable,
  });

  factory CharacterResult.fromGuideJson(Map<String, Object?> json) {
    return CharacterResult(
      character: _stringValue(json['pronunciationText']),
      score: 0,
      note: _stringValue(json['note']),
      kind: _stringValue(json['guideType'], fallback: 'NONE'),
      mouthGuideUrl: _nullableStringValue(json['mouthGuideUrl']),
      tongueGuideUrl: _nullableStringValue(json['tongueGuideUrl']),
      guideStatus: _nullableStringValue(json['guideStatus']),
      scoreStatus: ScoreStatus.unavailable,
    );
  }

  factory CharacterResult.fromResultJson(Map<String, Object?> json) {
    return CharacterResult(
      character: _stringValue(json['pronunciationText']),
      score: _intValue(json['score']),
      note: _stringValue(json['note']),
      kind: _stringValue(json['guideType'], fallback: 'NONE'),
      mouthGuideUrl: _nullableStringValue(json['mouthGuideUrl']),
      tongueGuideUrl: _nullableStringValue(json['tongueGuideUrl']),
      guideStatus: _nullableStringValue(json['guideStatus']),
      scoreStatus: ScoreStatus.fromWire(json['scoreStatus']),
    );
  }

  /// 평가 기록 DTO의 축약된 문자 field를 Result 화면이 사용하는 공통 형태로 변환한다.
  factory CharacterResult.fromHistoryJson(Map<String, Object?> json) {
    final rawScore = json['score'];
    return CharacterResult(
      character: _stringValue(json['text']),
      score: rawScore is num ? rawScore.round() : 0,
      note: _stringValue(json['feedback']),
      kind: 'NONE',
      mouthGuideUrl: _nullableStringValue(json['mouthGuideUrl']),
      tongueGuideUrl: _nullableStringValue(json['tongueGuideUrl']),
      scoreStatus: ScoreStatus.ofNullableScore(rawScore is num ? rawScore : null),
    );
  }

  final String character;
  final int score;
  final String note;
  final String kind;
  final String? mouthGuideUrl;
  final String? tongueGuideUrl;
  final String? guideStatus;
  final ScoreStatus scoreStatus;
}

String _stringValue(Object? value, {String fallback = ''}) {
  return value is String ? value : fallback;
}

String? _nullableStringValue(Object? value) {
  return value is String ? value : null;
}

int _intValue(Object? value) {
  return value is int ? value : 0;
}

int? _intOrNullValue(Object? value) {
  if (value is int) {
    return value;
  }

  if (value is num) {
    return value.toInt();
  }

  return null;
}
