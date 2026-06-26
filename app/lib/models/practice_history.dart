import 'practice_result.dart';
import 'practice_sentence.dart';

class PracticeHistory {
  const PracticeHistory({
    required this.items,
    required this.page,
    required this.size,
    required this.totalItems,
    required this.totalPages,
    required this.hasNext,
    this.bestScore,
  });

  factory PracticeHistory.fromJson(Map<String, Object?> json) {
    final itemsJson = json['items'];

    return PracticeHistory(
      items:
          itemsJson is List
              ? itemsJson
                  .whereType<Map<String, Object?>>()
                  .map(PracticeHistoryItem.fromJson)
                  .toList()
              : const [],
      page: _intValue(json['page']),
      size: _intValue(json['size']),
      totalItems: _intValue(json['totalItems']),
      totalPages: _intValue(json['totalPages']),
      hasNext: json['hasNext'] == true,
      bestScore: _nullableIntValue(json['bestScore']),
    );
  }

  final List<PracticeHistoryItem> items;
  final int page;
  final int size;
  final int totalItems;
  final int totalPages;
  final bool hasNext;
  final int? bestScore;
}

class PracticeHistoryItem {
  const PracticeHistoryItem({
    required this.evaluationLogId,
    this.sentenceId,
    required this.source,
    required this.originalText,
    required this.standardPronunciation,
    required this.recognizedText,
    required this.overallScore,
    required this.gradeLabel,
    required this.summary,
    required this.scoreBreakdown,
    required this.characters,
    this.createdAt,
  });

  factory PracticeHistoryItem.fromJson(Map<String, Object?> json) {
    final scoreBreakdownJson = json['scoreBreakdown'];
    final charactersJson = json['characters'];

    return PracticeHistoryItem(
      evaluationLogId: _intValue(json['evaluationLogId']),
      sentenceId: _nullableIntValue(json['sentenceId']),
      source: _stringValue(json['source'], fallback: 'CUSTOM'),
      originalText: _stringValue(json['originalText']),
      standardPronunciation: _stringValue(json['standardPronunciation']),
      recognizedText: _stringValue(json['recognizedText']),
      overallScore: _intValue(json['overallScore']),
      gradeLabel: _stringValue(json['gradeLabel']),
      summary: _stringValue(json['summary']),
      scoreBreakdown:
          scoreBreakdownJson is Map<String, Object?>
              ? PracticeScoreBreakdown.fromJson(scoreBreakdownJson)
              : const PracticeScoreBreakdown(
                accuracy: 0,
                fluency: 0,
                completeness: 0,
              ),
      characters:
          charactersJson is List
              ? charactersJson
                  .whereType<Map<String, Object?>>()
                  .map(CharacterResult.fromResultJson)
                  .toList()
              : const [],
      createdAt: _dateTimeValue(json['createdAt']),
    );
  }

  PracticeSentence toPracticeSentence() {
    return PracticeSentence(
      sentenceId: sentenceId,
      source: source,
      text: originalText,
      pronunciation: standardPronunciation,
      translation: 'Practice this sentence again.',
      level: source,
      category: 'History',
      point: summary,
      score: overallScore,
      characters: characters,
    );
  }

  final int evaluationLogId;
  final int? sentenceId;
  final String source;
  final String originalText;
  final String standardPronunciation;
  final String recognizedText;
  final int overallScore;
  final String gradeLabel;
  final String summary;
  final PracticeScoreBreakdown scoreBreakdown;
  final List<CharacterResult> characters;
  final DateTime? createdAt;
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

DateTime? _dateTimeValue(Object? value) {
  if (value is! String || value.trim().isEmpty) {
    return null;
  }

  return DateTime.tryParse(value);
}
