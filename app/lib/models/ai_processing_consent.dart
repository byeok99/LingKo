/// 서버 고지 버전이 앱의 안내와 다르면 전송을 허용하지 않는 동의 상태다.
class AiProcessingConsent {
  const AiProcessingConsent({
    required this.granted,
    required this.noticeVersion,
  });

  static const supportedVersion = '2026-09-12';
  final bool granted;
  final String noticeVersion;
  bool get isSupported => noticeVersion == supportedVersion;

  factory AiProcessingConsent.fromJson(Map<String, dynamic> json) {
    if (json['granted'] is! bool || json['noticeVersion'] is! String) {
      throw const FormatException('Invalid AI consent response');
    }
    return AiProcessingConsent(
      granted: json['granted'] as bool,
      noticeVersion: json['noticeVersion'] as String,
    );
  }
}

/// 동의 거절은 평가 장애가 아니며 로컬 녹음을 지우지 않고 재선택하게 한다.
class AiProcessingConsentDeclined implements Exception {}
