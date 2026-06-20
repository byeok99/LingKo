// 화면에 표시할 학습 문장 하나를 표현하는 임시 데이터 모델입니다.
// 나중에 백엔드 API가 붙으면 이 값들은 서버 응답 DTO와 매핑됩니다.
class PracticeSentence {
  const PracticeSentence({
    this.sentenceId,
    this.source = 'CUSTOM',
    required this.text,
    required this.pronunciation,
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
      text: _stringValue(sentence['originalText']),
      pronunciation: _stringValue(sentence['standardPronunciation']),
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
      text: _stringValue(json['originalText']),
      pronunciation: _stringValue(json['standardPronunciation']),
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
    final trimmed = text.trim();

    return PracticeSentence(
      source: 'CUSTOM',
      text: trimmed,
      pronunciation: 'Custom sentence',
      translation: 'Practice with your own sentence.',
      level: 'Custom',
      category: 'Free practice',
      point: 'Record your voice to receive pronunciation feedback.',
      score: 0,
      characters: _buildCustomCharacters(trimmed),
    );
  }

  final int? sentenceId;
  final String source;
  final String text;
  final String pronunciation;
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
class CharacterResult {
  const CharacterResult({
    required this.character,
    required this.score,
    required this.note,
    required this.kind,
    this.mouthGuideUrl,
    this.tongueGuideUrl,
    this.guideStatus,
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
    );
  }

  final String character;
  final int score;
  final String note;
  final String kind;
  final String? mouthGuideUrl;
  final String? tongueGuideUrl;
  final String? guideStatus;
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
