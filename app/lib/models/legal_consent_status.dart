// 파일 의도: 서버가 판정한 현재 약관 재동의 필요 여부를 앱 내부 값으로 표현한다.

/// 현재 서버 문서 버전과 사용자 동의 충족 여부를 나타낸다.
class LegalConsentStatus {
  const LegalConsentStatus({
    required this.required,
    required this.documentVersion,
  });

  factory LegalConsentStatus.fromJson(Map<String, Object?> json) {
    final required = json['required'];
    final documentVersion = json['documentVersion'];
    if (required is! bool ||
        documentVersion is! String ||
        documentVersion.isEmpty) {
      throw const FormatException('Invalid legal consent status');
    }
    return LegalConsentStatus(
      required: required,
      documentVersion: documentVersion,
    );
  }

  /// 현재 버전에 대한 동의 화면을 반드시 완료해야 하는지 여부다.
  final bool required;

  /// 서버가 현재로 인정하는 법무 문서 버전이다.
  final String documentVersion;
}
