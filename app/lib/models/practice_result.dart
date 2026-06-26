import 'practice_sentence.dart';

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
