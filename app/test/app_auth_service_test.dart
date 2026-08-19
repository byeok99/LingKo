// 파일 의도: app auth 서비스 test 기능의 사용자·API 계약과 회귀 조건을 검증한다.
// 선택 이유: 네트워크·플랫폼 의존성을 테스트 대역로 통제해 결과를 결정적으로 유지한다.

import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/api/api_client.dart';
import 'package:lingko_app/api/auth_api.dart';
import 'package:lingko_app/models/auth_session.dart';
import 'package:lingko_app/services/app_auth_service.dart';
import 'package:lingko_app/services/auth_session_store.dart';
import 'package:lingko_app/services/apple_identity_service.dart';
import 'package:lingko_app/services/google_identity_service.dart';

// 갱신 토큰 회전, single-flight 재시도, 로그아웃, 생명주기 경합을 검증한다.
void main() {
  test(
    'review access code session is saved without embedding credentials',
    () async {
      final store = AuthSessionStore(storage: MemoryTokenStorage());
      final authApi = FakeAuthApi();
      final service = DefaultAppAuthService(
        authApi: authApi,
        googleIdentityService: FakeGoogleIdentityService(),
        sessionStore: store,
      );

      final session = await service.signInForReview(
        'review-code-with-at-least-32-bytes',
      );

      expect(session, _session);
      expect(authApi.reviewAccessCodes, ['review-code-with-at-least-32-bytes']);
      expect(await store.read(), _session);
    },
  );

  test(
    'Apple credential is exchanged and the LingKo session is saved',
    () async {
      final store = AuthSessionStore(storage: MemoryTokenStorage());
      final authApi = FakeAuthApi();
      final service = DefaultAppAuthService(
        authApi: authApi,
        appleIdentityService: FakeAppleIdentityService(),
        googleIdentityService: FakeGoogleIdentityService(),
        sessionStore: store,
      );

      final session = await service.signInWithApple();

      expect(session, _session);
      expect(authApi.appleCredentials, [
        (
          'apple-identity-token',
          'raw-nonce-012345678901234567890123',
          'Apple Learner',
        ),
      ]);
      expect(await store.read(), _session);
    },
  );

  test('401 response refreshes the session and retries exactly once', () async {
    final store = AuthSessionStore(storage: MemoryTokenStorage());
    await store.save(_session);
    final authApi = FakeAuthApi(refreshedSession: _nextSession);
    final service = DefaultAppAuthService(
      authApi: authApi,
      googleIdentityService: FakeGoogleIdentityService(),
      sessionStore: store,
    );
    final attemptedTokens = <String>[];

    final result = await service.runAuthenticated((accessToken) async {
      attemptedTokens.add(accessToken);
      if (accessToken == _session.accessToken) {
        throw const ApiException('expired', statusCode: 401);
      }
      return 'ok';
    });

    expect(result, 'ok');
    expect(attemptedTokens, ['access.jwt', 'next-access.jwt']);
    expect(authApi.refreshTokens, ['refresh.jwt']);
    expect(await store.read(), _nextSession);
  });

  test('concurrent 401 responses share one refresh request', () async {
    final store = AuthSessionStore(storage: MemoryTokenStorage());
    await store.save(_session);
    final refreshCompleter = Completer<AuthSession>();
    final authApi = FakeAuthApi(refreshCompleter: refreshCompleter);
    final service = DefaultAppAuthService(
      authApi: authApi,
      googleIdentityService: FakeGoogleIdentityService(),
      sessionStore: store,
    );

    Future<String> request(String accessToken) async {
      if (accessToken == _session.accessToken) {
        throw const ApiException('expired', statusCode: 401);
      }
      return accessToken;
    }

    final first = service.runAuthenticated(request);
    final second = service.runAuthenticated(request);
    await Future<void>.delayed(Duration.zero);

    expect(authApi.refreshTokens, ['refresh.jwt']);
    refreshCompleter.complete(_nextSession);

    expect(await Future.wait([first, second]), [
      'next-access.jwt',
      'next-access.jwt',
    ]);
  });

  test('refresh failure clears local session and requires sign-in', () async {
    final store = AuthSessionStore(storage: MemoryTokenStorage());
    await store.save(_session);
    final authApi = FakeAuthApi(
      refreshError: const ApiException('revoked', statusCode: 401),
    );
    final service = DefaultAppAuthService(
      authApi: authApi,
      googleIdentityService: FakeGoogleIdentityService(),
      sessionStore: store,
    );

    expect(
      () => service.runAuthenticated<String>(
        (_) async => throw const ApiException('expired', statusCode: 401),
      ),
      throwsA(isA<AuthSessionExpiredException>()),
    );
    await Future<void>.delayed(Duration.zero);

    expect(await store.read(), isNull);
  });

  test(
    'signOut revokes the server session and always clears local tokens',
    () async {
      final store = AuthSessionStore(storage: MemoryTokenStorage());
      await store.save(_session);
      final authApi = FakeAuthApi(logoutError: const ApiException('offline'));
      final service = DefaultAppAuthService(
        authApi: authApi,
        googleIdentityService: FakeGoogleIdentityService(),
        sessionStore: store,
      );

      await expectLater(service.signOut(), throwsA(isA<ApiException>()));

      expect(authApi.logoutTokens, ['refresh.jwt']);
      expect(await store.read(), isNull);
    },
  );

  test('refresh response cannot restore a session after sign out', () async {
    final store = AuthSessionStore(storage: MemoryTokenStorage());
    await store.save(_session);
    final refreshCompleter = Completer<AuthSession>();
    final authApi = FakeAuthApi(refreshCompleter: refreshCompleter);
    final service = DefaultAppAuthService(
      authApi: authApi,
      googleIdentityService: FakeGoogleIdentityService(),
      sessionStore: store,
    );

    final request = service.runAuthenticated<String>((accessToken) async {
      if (accessToken == _session.accessToken) {
        throw const ApiException('expired', statusCode: 401);
      }
      return 'ok';
    });
    await _waitFor(() => authApi.refreshTokens.isNotEmpty);
    await service.signOut();
    refreshCompleter.complete(_nextSession);

    await expectLater(request, throwsA(isA<AuthSessionExpiredException>()));
    expect(await store.read(), isNull);
  });

  test('deleteAccount removes the server account and local session', () async {
    final store = AuthSessionStore(storage: MemoryTokenStorage());
    await store.save(_session);
    final authApi = FakeAuthApi();
    final service = DefaultAppAuthService(
      authApi: authApi,
      googleIdentityService: FakeGoogleIdentityService(),
      sessionStore: store,
    );

    await service.deleteAccount();

    expect(authApi.accountDeletionTokens, [('access.jwt', 'refresh.jwt')]);
    expect(await store.read(), isNull);
  });

  test('deleteAccount failure preserves the local session for retry', () async {
    final store = AuthSessionStore(storage: MemoryTokenStorage());
    await store.save(_session);
    final authApi = FakeAuthApi(
      accountDeletionError: const ApiException(
        'temporary storage failure',
        statusCode: 503,
      ),
    );
    final service = DefaultAppAuthService(
      authApi: authApi,
      googleIdentityService: FakeGoogleIdentityService(),
      sessionStore: store,
    );

    await expectLater(service.deleteAccount(), throwsA(isA<ApiException>()));

    expect(await store.read(), _session);
  });
}

Future<void> _waitFor(bool Function() condition) async {
  for (var attempt = 0; attempt < 20; attempt++) {
    if (condition()) {
      return;
    }
    await Future<void>.delayed(Duration.zero);
  }
  fail('Condition was not met');
}

const _session = AuthSession(
  tokenType: 'Bearer',
  accessToken: 'access.jwt',
  refreshToken: 'refresh.jwt',
  expiresInSeconds: 1800,
  user: AuthUser(userId: 7, email: 'user@example.com', name: 'LingKo User'),
);

const _nextSession = AuthSession(
  tokenType: 'Bearer',
  accessToken: 'next-access.jwt',
  refreshToken: 'next-refresh.jwt',
  expiresInSeconds: 1800,
  user: AuthUser(userId: 7, email: 'user@example.com', name: 'LingKo User'),
);

/// 테스트에서 Fake Auth Api 의존성을 결정적으로 대체한다.
/// 실제 네트워크·저장소·플랫폼 플러그인 없이 동일한 계약과 실패 경로를 재현하기 위해 명시적 테스트 대역을 선택했다.
class FakeAuthApi implements AuthApi {
  FakeAuthApi({
    this.refreshedSession,
    this.refreshCompleter,
    this.refreshError,
    this.logoutError,
    this.accountDeletionError,
  });

  final AuthSession? refreshedSession;
  final Completer<AuthSession>? refreshCompleter;
  final Object? refreshError;
  final Object? logoutError;
  final Object? accountDeletionError;
  final refreshTokens = <String>[];
  final logoutTokens = <String>[];
  final accountDeletionTokens = <(String, String)>[];
  final appleCredentials = <(String, String, String?)>[];
  final reviewAccessCodes = <String>[];

  @override
  Future<AuthSession> loginWithGoogleIdToken(String idToken) async => _session;

  @override
  Future<AuthSession> loginForReview(String accessCode) async {
    reviewAccessCodes.add(accessCode);
    return _session;
  }

  @override
  Future<AuthSession> loginWithAppleCredential({
    required String identityToken,
    required String rawNonce,
    String? displayName,
  }) async {
    appleCredentials.add((identityToken, rawNonce, displayName));
    return _session;
  }

  @override
  Future<AuthSession> refreshSession(String refreshToken) async {
    refreshTokens.add(refreshToken);
    if (refreshError != null) {
      throw refreshError!;
    }
    if (refreshCompleter != null) {
      return refreshCompleter!.future;
    }
    return refreshedSession!;
  }

  @override
  Future<void> logout(String refreshToken) async {
    logoutTokens.add(refreshToken);
    if (logoutError != null) {
      throw logoutError!;
    }
  }

  @override
  Future<void> deleteAccount(String accessToken, String refreshToken) async {
    accountDeletionTokens.add((accessToken, refreshToken));
    if (accountDeletionError != null) {
      throw accountDeletionError!;
    }
  }
}

/// 테스트에서 Fake Google Identity 서비스 의존성을 결정적으로 대체한다.
/// 실제 네트워크·저장소·플랫폼 플러그인 없이 동일한 계약과 실패 경로를 재현하기 위해 명시적 테스트 대역을 선택했다.
class FakeGoogleIdentityService implements GoogleIdentityService {
  @override
  Future<String> signInAndGetIdToken() async => 'google-id-token';
}

/// Apple 플랫폼 인증을 실제 계정 UI 없이 재현한다.
class FakeAppleIdentityService implements AppleIdentityService {
  @override
  Future<AppleIdentityCredential> signIn() async =>
      const AppleIdentityCredential(
        identityToken: 'apple-identity-token',
        rawNonce: 'raw-nonce-012345678901234567890123',
        displayName: 'Apple Learner',
      );
}

/// 테스트에서 Memory 토큰 저장소 의존성을 결정적으로 대체한다.
/// 실제 네트워크·저장소·플랫폼 플러그인 없이 동일한 계약과 실패 경로를 재현하기 위해 명시적 테스트 대역을 선택했다.
class MemoryTokenStorage implements TokenStorage {
  final values = <String, String>{};

  @override
  Future<void> delete({required String key}) async {
    values.remove(key);
  }

  @override
  Future<String?> read({required String key}) async => values[key];

  @override
  Future<void> write({required String key, required String value}) async {
    values[key] = value;
  }
}
