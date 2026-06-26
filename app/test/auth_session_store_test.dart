import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/models/auth_session.dart';
import 'package:lingko_app/services/auth_session_store.dart';

void main() {
  test('AuthSessionStore saves and clears token session data', () async {
    final storage = MemoryTokenStorage();
    final store = AuthSessionStore(storage: storage);
    const session = AuthSession(
      tokenType: 'Bearer',
      accessToken: 'access.jwt',
      refreshToken: 'refresh.jwt',
      expiresInSeconds: 1800,
      user: AuthUser(
        userId: 7,
        email: 'user@example.com',
        name: 'LingKo User',
        profileImageUrl: 'https://example.com/profile.png',
      ),
    );

    await store.save(session);

    expect(storage.values['auth.accessToken'], 'access.jwt');
    expect(storage.values['auth.refreshToken'], 'refresh.jwt');
    expect(storage.values['auth.userId'], '7');
    expect(storage.values['auth.email'], 'user@example.com');
    expect(await store.read(), session);

    await store.clear();

    expect(storage.values, isEmpty);
    expect(await store.read(), isNull);
  });
}

class MemoryTokenStorage implements TokenStorage {
  final values = <String, String>{};

  @override
  Future<String?> read({required String key}) async {
    return values[key];
  }

  @override
  Future<void> write({required String key, required String value}) async {
    values[key] = value;
  }

  @override
  Future<void> delete({required String key}) async {
    values.remove(key);
  }
}
