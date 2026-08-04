// 파일 의도: practice result의 앱 내부 데이터 의미와 API 매핑을 정의한다.
// 선택 이유: 동적 JSON을 형식이 지정된 model로 변환해 잘못된 응답을 UI 경계 전에 차단한다.

import 'practice_sentence.dart';

/// Practice Result 값의 의미와 불변 데이터 구조를 나타낸다.
/// UI가 Map key나 nullable JSON 세부사항을 직접 다루지 않도록 형식이 지정된 model을 선택했다.
class PracticeResult {
  const PracticeResult({
    required this.overallScore,
    required this.gradeLabel,
    required this.summary,
    this.recognizedText = '',
    this.characterScoreStatus = 'UNAVAILABLE',
    this.wordScoreStatus = 'UNAVAILABLE',
    required this.scoreBreakdown,
    required this.weakCharacters,
    required this.characters,
    this.words = const [],
  });

  factory PracticeResult.fromJson(Map<String, Object?> json) {
    final scoreBreakdownJson = json['scoreBreakdown'];
    final weakCharactersJson = json['weakCharacters'];
    final charactersJson = json['characters'];
    final wordsJson = json['words'];

    return PracticeResult(
      overallScore: _intValue(json['overallScore']),
      gradeLabel: _stringValue(json['gradeLabel']),
      summary: _stringValue(json['summary']),
      recognizedText: _stringValue(json['recognizedText']),
      characterScoreStatus: _stringValue(
        json['characterScoreStatus'],
        fallback: 'UNAVAILABLE',
      ),
      wordScoreStatus: _stringValue(
        json['wordScoreStatus'],
        fallback: 'UNAVAILABLE',
      ),
      scoreBreakdown:
          scoreBreakdownJson is Map<String, Object?>
              ? PracticeScoreBreakdown.fromJson(scoreBreakdownJson)
              : const PracticeScoreBreakdown(
                accuracy: 0,
                fluency: 0,
                completeness: 0,
              ),
      weakCharacters: _charactersFromJson(weakCharactersJson),
      characters: _charactersFromJson(charactersJson),
      words: _wordsFromJson(wordsJson, history: false),
    );
  }

  final int overallScore;
  final String gradeLabel;
  final String summary;
  final String recognizedText;
  final String characterScoreStatus;
  final String wordScoreStatus;
  final PracticeScoreBreakdown scoreBreakdown;
  final List<CharacterResult> weakCharacters;
  final List<CharacterResult> characters;
  final List<PracticeWordResult> words;
}

/// 공급자가 신뢰할 수 있게 제공한 단어 점수와 guide-only 음절 목록을 묶는다.
class PracticeWordResult {
  const PracticeWordResult({
    required this.position,
    required this.text,
    this.score,
    this.scoreStatus = 'UNAVAILABLE',
    required this.syllables,
  });

  factory PracticeWordResult.fromResultJson(Map<String, Object?> json) {
    return PracticeWordResult._fromJson(json, history: false);
  }

  factory PracticeWordResult.fromHistoryJson(Map<String, Object?> json) {
    return PracticeWordResult._fromJson(json, history: true);
  }

  factory PracticeWordResult._fromJson(
    Map<String, Object?> json, {
    required bool history,
  }) {
    final syllablesJson = json['syllables'];
    return PracticeWordResult(
      position: _intValue(json['position']),
      text: _stringValue(json['text']),
      score: _nullableIntValue(json['score']),
      scoreStatus: _stringValue(
        json['scoreStatus'],
        fallback: json['score'] == null ? 'UNAVAILABLE' : 'AVAILABLE',
      ),
      syllables:
          syllablesJson is List
              ? syllablesJson
                  .whereType<Map<String, Object?>>()
                  .map(
                    history
                        ? CharacterResult.fromHistoryJson
                        : CharacterResult.fromResultJson,
                  )
                  .toList(growable: false)
              : const [],
    );
  }

  final int position;
  final String text;
  final int? score;
  final String scoreStatus;
  final List<CharacterResult> syllables;
}

/// Practice Score Breakdown 값의 의미와 불변 데이터 구조를 나타낸다.
/// UI가 Map key나 nullable JSON 세부사항을 직접 다루지 않도록 형식이 지정된 model을 선택했다.
class PracticeScoreBreakdown {
  const PracticeScoreBreakdown({
    required this.accuracy,
    required this.fluency,
    required this.completeness,
  });

  factory PracticeScoreBreakdown.fromJson(Map<String, Object?> json) {
    return PracticeScoreBreakdown(
      accuracy: _intValue(json['accuracy']),
      fluency: _intValue(json['fluency']),
      completeness: _intValue(json['completeness']),
    );
  }

  final int accuracy;
  final int fluency;
  final int completeness;
}

List<CharacterResult> _charactersFromJson(Object? value) {
  return value is List
      ? value
          .whereType<Map<String, Object?>>()
          .map(CharacterResult.fromResultJson)
          .toList()
      : const [];
}

List<PracticeWordResult> _wordsFromJson(
  Object? value, {
  required bool history,
}) {
  return value is List
      ? value
          .whereType<Map<String, Object?>>()
          .map(
            history
                ? PracticeWordResult.fromHistoryJson
                : PracticeWordResult.fromResultJson,
          )
          .toList(growable: false)
      : const [];
}

String _stringValue(Object? value, {String fallback = ''}) {
  return value is String ? value : fallback;
}

int _intValue(Object? value) {
  if (value is int) {
    return value;
  }

  if (value is num) {
    return value.round();
  }

  return 0;
}

int? _nullableIntValue(Object? value) {
  if (value is int) {
    return value;
  }
  if (value is num) {
    return value.round();
  }
  return null;
}
