// 파일 의도: user preferences의 앱 내부 데이터 의미와 API 매핑을 정의한다.
// 선택 이유: 동적 JSON을 형식이 지정된 model로 변환해 잘못된 응답을 UI 경계 전에 차단한다.

/// User Preferences 값의 의미와 불변 데이터 구조를 나타낸다.
/// UI가 Map key나 nullable JSON 세부사항을 직접 다루지 않도록 형식이 지정된 model을 선택했다.
class UserPreferences {
  const UserPreferences({
    required this.displayLanguage,
    required this.nativeLanguage,
  });

  factory UserPreferences.fromJson(Map<String, Object?> json) {
    return UserPreferences(
      displayLanguage: _stringValue(json['displayLanguage'], fallback: 'en'),
      nativeLanguage: _stringValue(json['nativeLanguage'], fallback: 'en'),
    );
  }

  static const defaults = UserPreferences(
    displayLanguage: 'en',
    nativeLanguage: 'en',
  );

  final String displayLanguage;
  final String nativeLanguage;

  Map<String, Object?> toJson() {
    return {
      'displayLanguage': displayLanguage,
      'nativeLanguage': nativeLanguage,
    };
  }

  UserPreferences copyWith({String? displayLanguage, String? nativeLanguage}) {
    return UserPreferences(
      displayLanguage: displayLanguage ?? this.displayLanguage,
      nativeLanguage: nativeLanguage ?? this.nativeLanguage,
    );
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        other is UserPreferences &&
            other.displayLanguage == displayLanguage &&
            other.nativeLanguage == nativeLanguage;
  }

  @override
  int get hashCode => Object.hash(displayLanguage, nativeLanguage);
}

String _stringValue(Object? value, {required String fallback}) {
  if (value is String && value.trim().isNotEmpty) {
    return value;
  }

  return fallback;
}
