// 파일 의도: auth session store 플랫폼·생명주기 기능을 추상화한다.
// 선택 이유: 플러그인 세부사항을 UI에서 격리해 오류 처리와 테스트 대역 교체를 가능하게 한다.

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../models/auth_session.dart';

/// 토큰 저장소 플랫폼 기능 계약을 정의한다.
/// 플러그인 구현을 교체하고 unit test에서 대역을 주입할 수 있도록 추상 경계를 선택했다.
abstract class TokenStorage {
  Future<void> write({required String key, required String value});

  Future<String?> read({required String key});

  Future<void> delete({required String key});
}

/// Flutter Secure 토큰 저장소 플랫폼·세션 생명주기 동작을 구현한다.
/// UI가 플러그인 API와 보안 저장 세부사항에 직접 결합되지 않도록 서비스에 캡슐화했다.
class FlutterSecureTokenStorage implements TokenStorage {
  FlutterSecureTokenStorage({FlutterSecureStorage? storage})
    : _storage = storage ?? const FlutterSecureStorage();

  final FlutterSecureStorage _storage;

  @override
  Future<void> write({required String key, required String value}) {
    return _storage.write(key: key, value: value);
  }

  @override
  Future<String?> read({required String key}) {
    return _storage.read(key: key);
  }

  @override
  Future<void> delete({required String key}) {
    return _storage.delete(key: key);
  }
}

/// Auth Session Store 플랫폼·세션 생명주기 동작을 구현한다.
/// UI가 플러그인 API와 보안 저장 세부사항에 직접 결합되지 않도록 서비스에 캡슐화했다.
class AuthSessionStore {
  AuthSessionStore({TokenStorage? storage})
    : _storage = storage ?? FlutterSecureTokenStorage();

  static const _accessTokenKey = 'auth.accessToken';
  static const _refreshTokenKey = 'auth.refreshToken';
  static const _tokenTypeKey = 'auth.tokenType';
  static const _expiresInSecondsKey = 'auth.expiresInSeconds';
  static const _userIdKey = 'auth.userId';
  static const _emailKey = 'auth.email';
  static const _nameKey = 'auth.name';
  static const _profileImageUrlKey = 'auth.profileImageUrl';

  final TokenStorage _storage;

  Future<void> save(AuthSession session) async {
    // 인증 정보는 일반 환경 설정과 분리해 운영체제의 보안 저장소에만 기록한다.
    await _storage.write(key: _accessTokenKey, value: session.accessToken);
    await _storage.write(key: _refreshTokenKey, value: session.refreshToken);
    await _storage.write(key: _tokenTypeKey, value: session.tokenType);
    await _storage.write(
      key: _expiresInSecondsKey,
      value: '${session.expiresInSeconds}',
    );
    await _storage.write(key: _userIdKey, value: '${session.user.userId}');
    await _storage.write(key: _emailKey, value: session.user.email);
    await _storage.write(key: _nameKey, value: session.user.name);

    final profileImageUrl = session.user.profileImageUrl;
    if (profileImageUrl != null && profileImageUrl.isNotEmpty) {
      await _storage.write(key: _profileImageUrlKey, value: profileImageUrl);
    } else {
      await _storage.delete(key: _profileImageUrlKey);
    }
  }

  Future<AuthSession?> read() async {
    final accessToken = await _storage.read(key: _accessTokenKey);
    final refreshToken = await _storage.read(key: _refreshTokenKey);
    final userId = await _storage.read(key: _userIdKey);

    // 필수 값 중 하나라도 없으면 부분 저장된 세션을 인증 상태로 복구하지 않는다.
    if (accessToken == null || refreshToken == null || userId == null) {
      return null;
    }

    return AuthSession(
      tokenType: await _storage.read(key: _tokenTypeKey) ?? 'Bearer',
      accessToken: accessToken,
      refreshToken: refreshToken,
      expiresInSeconds:
          int.tryParse(await _storage.read(key: _expiresInSecondsKey) ?? '') ??
          0,
      user: AuthUser(
        userId: int.tryParse(userId) ?? 0,
        email: await _storage.read(key: _emailKey) ?? '',
        name: await _storage.read(key: _nameKey) ?? '',
        profileImageUrl: await _storage.read(key: _profileImageUrlKey),
      ),
    );
  }

  Future<void> clear() async {
    // 로그아웃 후 사용자 식별 정보가 남지 않도록 세션을 구성하는 모든 키를 함께 삭제한다.
    await _storage.delete(key: _accessTokenKey);
    await _storage.delete(key: _refreshTokenKey);
    await _storage.delete(key: _tokenTypeKey);
    await _storage.delete(key: _expiresInSecondsKey);
    await _storage.delete(key: _userIdKey);
    await _storage.delete(key: _emailKey);
    await _storage.delete(key: _nameKey);
    await _storage.delete(key: _profileImageUrlKey);
  }
}
