// 파일 의도: auth session store test 기능의 사용자·API 계약과 회귀 조건을 검증한다.
// 선택 이유: 네트워크·플랫폼 의존성을 테스트 대역로 통제해 결과를 결정적으로 유지한다.

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

/// 테스트에서 Memory 토큰 저장소 의존성을 결정적으로 대체한다.
/// 실제 네트워크·저장소·플랫폼 플러그인 없이 동일한 계약과 실패 경로를 재현하기 위해 명시적 테스트 대역을 선택했다.
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
