// 파일 의도: 회원가입 동의 화면에서 사용자가 고른 항목을 하나의 값으로 담는다.
// 선택 이유: 동의는 나중에 "언제·어느 문서 버전에 동의했는지"를 증명해야 하는 기록이라
// 화면 상태에 흩어두지 않고 전달·저장 가능한 불변 값으로 모았다.

/// 동의 화면이 보여주는 문서 종류다.
///
/// 화면은 이 값만 상위로 올려보내고, 실제로 어떤 URL이나 화면을 여는지는 상위가 정한다.
/// 문서 위치가 바뀌어도 화면을 고치지 않기 위한 경계다.
enum ConsentDocument {
  /// 이용약관. 가입 시 필수 동의 대상이다.
  termsOfService,

  /// 개인정보 처리방침. 동의가 아니라 확인 대상이다.
  ///
  /// 처리방침은 「개인정보 보호법」상 동의를 받는 문서가 아니라 공개하는 문서이고,
  /// 계약 이행에 필요한 필수 항목의 수집·이용은 같은 법 제15조 제1항 제4호에 따라
  /// 별도 동의 없이 가능하다. 그래서 화면 문구도 "동의"가 아닌 "확인"으로 둔다.
  privacyPolicy,
}

/// 회원가입 시점에 사용자가 고른 동의 항목이다.
///
/// 필수 두 항목이 모두 참일 때만 가입을 진행할 수 있다. 선택 항목인 마케팅 수신은
/// 거부해도 서비스 이용에 제한이 없어야 하므로 가입 가능 여부 판단에 넣지 않는다.
class ConsentSelection {
  const ConsentSelection({
    required this.termsAgreed,
    required this.privacyAcknowledged,
    required this.marketingOptIn,
    required this.documentVersion,
    required this.agreedAt,
  });

  /// [필수] 이용약관에 동의했는지 여부다.
  final bool termsAgreed;

  /// [필수] 개인정보 처리 관련 내용을 확인했는지 여부다.
  final bool privacyAcknowledged;

  /// [선택] 마케팅 정보 수신에 동의했는지 여부다.
  ///
  /// 거부(false)와 미선택을 구분하지 않는다. 화면이 기본값 false로 시작하고
  /// 사용자가 켜야만 true가 되므로, false는 언제나 "수신하지 않음"을 뜻한다.
  final bool marketingOptIn;

  /// 동의 대상 문서의 버전이다. 문서의 시행일을 그대로 쓴다.
  ///
  /// 약관이 개정되면 어떤 버전에 동의했는지를 근거로 재동의 대상을 가려야 하므로
  /// 동의 사실만 남기지 않고 버전을 함께 기록한다.
  final String documentVersion;

  /// 사용자가 동의 버튼을 누른 시각이다. 기기 시각 기준이다.
  ///
  /// 분쟁 시 증거로 쓰는 값은 서버가 수신 시점에 다시 기록해야 한다.
  /// 기기 시각은 사용자가 바꿀 수 있어 단독 근거로 삼지 않는다.
  final DateTime agreedAt;

  /// 가입을 진행할 수 있는 상태인지 여부다.
  bool get canProceed => termsAgreed && privacyAcknowledged;

  Map<String, Object?> toJson() => {
    'termsAgreed': termsAgreed,
    'privacyAcknowledged': privacyAcknowledged,
    'marketingOptIn': marketingOptIn,
    'documentVersion': documentVersion,
    'agreedAt': agreedAt.toUtc().toIso8601String(),
  };
}

/// 현재 앱이 보여주는 약관·처리방침의 버전이다.
///
/// `docs/legal/`의 네 문서에 적힌 시행일과 같은 값을 유지한다.
/// 문서를 개정하면 이 상수도 같은 작업에서 올려야 재동의 대상을 가릴 수 있다.
const String consentDocumentVersion = '2026-08-07';
