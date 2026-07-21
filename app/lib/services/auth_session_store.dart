import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../models/auth_session.dart';

abstract class TokenStorage {
  Future<void> write({required String key, required String value});

  Future<String?> read({required String key});

  Future<void> delete({required String key});
}

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
