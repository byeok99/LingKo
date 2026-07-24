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
    required this.scoreBreakdown,
    required this.weakCharacters,
    required this.characters,
  });

  factory PracticeResult.fromJson(Map<String, Object?> json) {
    final scoreBreakdownJson = json['scoreBreakdown'];
    final weakCharactersJson = json['weakCharacters'];
    final charactersJson = json['characters'];

    return PracticeResult(
      overallScore: _intValue(json['overallScore']),
      gradeLabel: _stringValue(json['gradeLabel']),
      summary: _stringValue(json['summary']),
      recognizedText: _stringValue(json['recognizedText']),
      characterScoreStatus: _stringValue(
        json['characterScoreStatus'],
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
    );
  }

  final int overallScore;
  final String gradeLabel;
  final String summary;
  final String recognizedText;
  final String characterScoreStatus;
  final PracticeScoreBreakdown scoreBreakdown;
  final List<CharacterResult> weakCharacters;
  final List<CharacterResult> characters;
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
