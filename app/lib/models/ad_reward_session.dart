// 파일 의도: 서버가 발급하고 Google SSV가 완료하는 광고 보상 session 상태를 정의한다.

/// AdMob customData에 넣을 1회성 서버 token이다.
class AdRewardSession {
  const AdRewardSession({required this.sessionToken, required this.expiresAt});

  final String sessionToken;
  final DateTime expiresAt;

  factory AdRewardSession.fromJson(Map<String, Object?> json) {
    final token = json['sessionToken'];
    final expiresAt = json['expiresAt'];
    if (token is! String || token.isEmpty || expiresAt is! String) {
      throw const FormatException('Invalid ad reward session');
    }
    return AdRewardSession(
      sessionToken: token,
      expiresAt: DateTime.parse(expiresAt),
    );
  }
}

/// 광고 보상 session이 signed callback을 기다리는지, 완료됐는지, 만료됐는지를 나타낸다.
enum AdRewardStatus { pending, completed, expired }

/// 서버가 signed callback을 처리했는지 polling하는 상태다.
class AdRewardSessionStatus {
  const AdRewardSessionStatus({required this.status, required this.credited});

  final AdRewardStatus status;
  final bool? credited;

  factory AdRewardSessionStatus.fromJson(Map<String, Object?> json) {
    final rawStatus = json['status'];
    final credited = json['credited'];
    if (rawStatus is! String || (credited != null && credited is! bool)) {
      throw const FormatException('Invalid ad reward session status');
    }
    final status = switch (rawStatus) {
      'PENDING' => AdRewardStatus.pending,
      'COMPLETED' => AdRewardStatus.completed,
      'EXPIRED' => AdRewardStatus.expired,
      _ => throw const FormatException('Invalid ad reward status'),
    };
    return AdRewardSessionStatus(status: status, credited: credited as bool?);
  }
}
