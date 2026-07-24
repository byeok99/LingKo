// 파일 의도: auth session의 앱 내부 데이터 의미와 API 매핑을 정의한다.
// 선택 이유: 동적 JSON을 형식이 지정된 model로 변환해 잘못된 응답을 UI 경계 전에 차단한다.

/// Auth Session 값의 의미와 불변 데이터 구조를 나타낸다.
/// UI가 Map key나 nullable JSON 세부사항을 직접 다루지 않도록 형식이 지정된 model을 선택했다.
class AuthSession {
  const AuthSession({
    required this.tokenType,
    required this.accessToken,
    required this.refreshToken,
    required this.expiresInSeconds,
    required this.user,
  });

  factory AuthSession.fromJson(Map<String, Object?> json) {
    final userJson = json['user'];

    if (userJson is! Map<String, Object?>) {
      throw const FormatException('Missing auth user');
    }

    return AuthSession(
      tokenType: _stringValue(json['tokenType'], fallback: 'Bearer'),
      accessToken: _stringValue(json['accessToken']),
      refreshToken: _stringValue(json['refreshToken']),
      expiresInSeconds: _intValue(json['expiresInSeconds']),
      user: AuthUser.fromJson(userJson),
    );
  }

  final String tokenType;
  final String accessToken;
  final String refreshToken;
  final int expiresInSeconds;
  final AuthUser user;

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        other is AuthSession &&
            other.tokenType == tokenType &&
            other.accessToken == accessToken &&
            other.refreshToken == refreshToken &&
            other.expiresInSeconds == expiresInSeconds &&
            other.user == user;
  }

  @override
  int get hashCode {
    return Object.hash(
      tokenType,
      accessToken,
      refreshToken,
      expiresInSeconds,
      user,
    );
  }
}

/// Auth User 값의 의미와 불변 데이터 구조를 나타낸다.
/// UI가 Map key나 nullable JSON 세부사항을 직접 다루지 않도록 형식이 지정된 model을 선택했다.
class AuthUser {
  const AuthUser({
    required this.userId,
    required this.email,
    required this.name,
    this.profileImageUrl,
  });

  factory AuthUser.fromJson(Map<String, Object?> json) {
    return AuthUser(
      userId: _intValue(json['userId']),
      email: _stringValue(json['email']),
      name: _stringValue(json['name']),
      profileImageUrl: _nullableStringValue(json['profileImageUrl']),
    );
  }

  final int userId;
  final String email;
  final String name;
  final String? profileImageUrl;

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        other is AuthUser &&
            other.userId == userId &&
            other.email == email &&
            other.name == name &&
            other.profileImageUrl == profileImageUrl;
  }

  @override
  int get hashCode {
    return Object.hash(userId, email, name, profileImageUrl);
  }
}

String _stringValue(Object? value, {String fallback = ''}) {
  return value is String ? value : fallback;
}

String? _nullableStringValue(Object? value) {
  return value is String ? value : null;
}

int _intValue(Object? value) {
  if (value is int) {
    return value;
  }

  if (value is num) {
    return value.round();
  }

  return 0;
}
