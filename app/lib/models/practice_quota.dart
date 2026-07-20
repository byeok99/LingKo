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
