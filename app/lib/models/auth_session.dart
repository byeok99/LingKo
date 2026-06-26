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
