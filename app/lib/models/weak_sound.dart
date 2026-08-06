// 파일 의도: 반복해서 틀리는 음절의 누적 성적과 상세 자료를 앱 모델로 정의한다.
// 선택 이유: 점수는 어절에서만 측정되지만 학습 단위는 음절이라, 서버가 어절 점수를
// 그 어절의 음절들에 귀속시켜 내려준다. 앱은 그 결과를 그대로 표시하고 다시 계산하지 않는다.

import 'practice_sentence.dart';

/// Home 타일과 상세 화면 머리말에 쓰는 음절별 누적 성적이다.
///
/// [averageScore]는 이 음절이 들어간 어절 점수들의 가중 평균이지, 음절 자체를 측정한
/// 점수가 아니다. 화면 문구도 "이 음절이 든 연습들의 평균"으로 읽히게 써야 한다.
class WeakSound {
  const WeakSound({
    required this.text,
    required this.romanization,
    required this.averageScore,
    required this.attemptCount,
  });

  factory WeakSound.fromJson(Map<String, Object?> json) {
    return WeakSound(
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

/// 음절 하나를 파고드는 화면이 필요로 하는 자료 묶음이다.
///
/// 누적 성적과 두 목록을 한 번에 받는 이유는, 따로 조회하면 머리말의 평균·횟수와
/// 아래 목록의 개수가 어긋나 보일 수 있기 때문이다.
class SoundDetail {
  const SoundDetail({
    required this.text,
    required this.romanization,
    required this.averageScore,
    required this.attemptCount,
    required this.practiced,
    required this.suggested,
  });

  factory SoundDetail.fromJson(Map<String, Object?> json) {
    return SoundDetail(
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

  /// 이 음절이 들어간 어절을 연습했던 과거 시도다.
  final List<PracticedAttempt> practiced;

  /// 이 음절이 들어있지만 아직 연습하지 않은 추천 문장이다.
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

  /// 음절이 아니라 그 음절이 속한 어절의 점수다.
  /// 점수를 신뢰할 수 없어 저장하지 않은 시도는 null이며 0점과 구분한다.
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
