// 파일 의도: 로그인 전에 서버가 공개하는 현재 법무 문서 버전을 표현한다.

/// 동의 화면이 제출 body를 만들 때 따라야 하는 서버 기준 문서 버전이다.
class LegalConsentPolicy {
  const LegalConsentPolicy({required this.documentVersion});

  factory LegalConsentPolicy.fromJson(Map<String, Object?> json) {
    final documentVersion = json['documentVersion'];
    if (documentVersion is! String || documentVersion.trim().isEmpty) {
      throw const FormatException('Invalid legal consent policy');
    }
    return LegalConsentPolicy(documentVersion: documentVersion);
  }

  /// 서버가 새 동의로 인정하는 공개 법무 문서 버전이다.
  final String documentVersion;
}
