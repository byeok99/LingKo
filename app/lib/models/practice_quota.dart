// 파일 의도: practice 할당량의 앱 내부 데이터 의미와 API 매핑을 정의한다.
// 선택 이유: 동적 JSON을 형식이 지정된 model로 변환해 잘못된 응답을 UI 경계 전에 차단한다.

/// Practice 할당량 값의 의미와 불변 데이터 구조를 나타낸다.
/// UI가 Map key나 nullable JSON 세부사항을 직접 다루지 않도록 형식이 지정된 model을 선택했다.
class PracticeQuota {
  const PracticeQuota({
    required this.date,
    required this.freeLimit,
    required this.freeUsed,
    required this.rewardedAvailable,
    required this.remainingPractices,
    required this.resetAt,
  });

  final String date;
  final int freeLimit;
  final int freeUsed;
  final int rewardedAvailable;
  final int remainingPractices;
  final DateTime? resetAt;

  bool get hasRemainingPractice => remainingPractices > 0;

  factory PracticeQuota.fromJson(Map<String, Object?> json) {
    return PracticeQuota(
      date: _readString(json, 'date'),
      freeLimit: _readInt(json, 'freeLimit'),
      freeUsed: _readInt(json, 'freeUsed'),
      rewardedAvailable: _readInt(json, 'rewardedAvailable'),
      remainingPractices: _readInt(json, 'remainingPractices'),
      resetAt: _readDateTime(json, 'resetAt'),
    );
  }
}

String _readString(Map<String, Object?> json, String key) {
  final value = json[key];
  if (value is String) {
    return value;
  }
  throw FormatException('Invalid $key');
}

int _readInt(Map<String, Object?> json, String key) {
  final value = json[key];
  if (value is int) {
    return value;
  }
  if (value is num) {
    return value.toInt();
  }
  throw FormatException('Invalid $key');
}

DateTime? _readDateTime(Map<String, Object?> json, String key) {
  final value = json[key];
  if (value == null) {
    return null;
  }
  if (value is String) {
    return DateTime.parse(value);
  }
  throw FormatException('Invalid $key');
}
