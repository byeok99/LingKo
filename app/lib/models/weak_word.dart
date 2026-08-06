// 파일 의도: 반복해서 틀리는 어절의 누적 성적과 상세 자료를 앱 모델로 정의한다.
// 선택 이유: 음절이 아니라 어절 단위인 것은 신뢰할 수 있는 점수의 최소 단위가 어절이기 때문이다.

import 'practice_sentence.dart';

/// Home 타일과 상세 화면 머리말에 쓰는 어절별 누적 성적이다.
class WeakWord {
  const WeakWord({
    required this.text,
    required this.romanization,
    required this.averageScore,
    required this.attemptCount,
  });

  factory WeakWord.fromJson(Map<String, Object?> json) {
    return WeakWord(
      text: _string(json['text']),
      romanization: _string(json['romanization']),
      averageScore: _int(json['averageScore']),
      attemptCount: _int(json['attemptCount']),
    );
  }

  final String text;
  final String romanization;

  /// 서버가 반올림한 표시용 정수다. 화면이 다시 계산하지 않는다.
  final int averageScore;
  final int attemptCount;
}

/// 어절 하나를 파고드는 화면이 필요로 하는 자료 묶음이다.
///
/// 누적 성적과 두 목록을 한 번에 받는 이유는, 따로 조회하면 머리말의 평균·횟수와
/// 아래 목록의 개수가 어긋나 보일 수 있기 때문이다.
class WordDetail {
  const WordDetail({
    required this.text,
    required this.romanization,
    required this.averageScore,
    required this.attemptCount,
    required this.practiced,
    required this.suggested,
  });

  factory WordDetail.fromJson(Map<String, Object?> json) {
    return WordDetail(
      text: _string(json['text']),
      romanization: _string(json['romanization']),
      averageScore: _int(json['averageScore']),
      attemptCount: _int(json['attemptCount']),
      practiced: _list(json['practiced'], PracticedAttempt.fromJson),
      suggested: _list(json['suggested'], PracticeSentence.fromSuggestedJson),
    );
  }

  final String text;
  final String romanization;
  final int averageScore;
  final int attemptCount;

  /// 이 어절을 연습했던 과거 시도다.
  final List<PracticedAttempt> practiced;

  /// 이 어절이 들어있지만 아직 연습하지 않은 추천 문장이다.
  final List<PracticeSentence> suggested;
}

/// 과거 시도 한 건이다. 행을 누르면 그때의 결과로 이동한다.
class PracticedAttempt {
  const PracticedAttempt({
    required this.evaluationLogId,
    required this.originalText,
    required this.romanization,
    required this.score,
    required this.createdAt,
  });

  factory PracticedAttempt.fromJson(Map<String, Object?> json) {
    return PracticedAttempt(
      evaluationLogId: json['evaluationLogId'] is num
          ? (json['evaluationLogId']! as num).toInt()
          : null,
      originalText: _string(json['originalText']),
      romanization: _string(json['romanization']),
      score: json['score'] is num ? (json['score']! as num).round() : null,
      createdAt: DateTime.tryParse(_string(json['createdAt'])),
    );
  }

  final int? evaluationLogId;
  final String originalText;
  final String romanization;

  /// 점수를 신뢰할 수 없어 저장하지 않은 시도는 null이다.
  final int? score;
  final DateTime? createdAt;
}

String _string(Object? value) => value is String ? value : '';

int _int(Object? value) => value is num ? value.round() : 0;

List<T> _list<T>(Object? value, T Function(Map<String, Object?>) map) {
  if (value is! List) {
    return const [];
  }
  return value.whereType<Map<String, Object?>>().map(map).toList(growable: false);
}
