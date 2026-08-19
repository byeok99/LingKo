// 파일 의도: auth api test 기능의 사용자·API 계약과 회귀 조건을 검증한다.
// 선택 이유: 네트워크·플랫폼 의존성을 테스트 대역로 통제해 결과를 결정적으로 유지한다.

import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/api/api_client.dart';
import 'package:lingko_app/api/auth_api.dart';

// 로그인, 갱신 회전, 로그아웃의 JSON 계약을 검증한다.
void main() {
  test('loginWithGoogleIdToken posts OAuth request and maps session', () async {
    Uri? requestedUri;
    Map<String, Object?>? requestedBody;
    final api = DartIoAuthApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        postJsonTransport: (uri, body, timeout) async {
          requestedUri = uri;
          requestedBody = body;

          return ApiResponse(
            statusCode: 200,
            body: jsonEncode({
              'tokenType': 'Bearer',
              'accessToken': 'access.jwt',
              'refreshToken': 'refresh.jwt',
              'expiresInSeconds': 1800,
              'user': {
                'userId': 7,
                'email': 'user@example.com',
                'name': 'LingKo User',
                'profileImageUrl': 'https://example.com/profile.png',
              },
            }),
          );
        },
      ),
    );

    final session = await api.loginWithGoogleIdToken('google-id-token');

    expect(
      requestedUri.toString(),
      'http://localhost:8080/api/auth/oauth/login',
    );
    expect(requestedBody, {'provider': 'GOOGLE', 'idToken': 'google-id-token'});
    expect(session.tokenType, 'Bearer');
    expect(session.accessToken, 'access.jwt');
    expect(session.refreshToken, 'refresh.jwt');
    expect(session.user.userId, 7);
    expect(session.user.email, 'user@example.com');
  });

  test(
    'loginWithAppleCredential posts token, raw nonce, and first name',
    () async {
      Map<String, Object?>? requestedBody;
      final api = DartIoAuthApi(
        client: ApiClient(
          baseUrl: 'http://localhost:8080',
          postJsonTransport: (uri, body, timeout) async {
            requestedBody = body;
            return ApiResponse(
              statusCode: 200,
              body: jsonEncode({
                'tokenType': 'Bearer',
                'accessToken': 'access.jwt',
                'refreshToken': 'refresh.jwt',
                'expiresInSeconds': 1800,
                'user': {'userId': 9, 'name': 'Apple Learner'},
              }),
            );
          },
        ),
      );

      await api.loginWithAppleCredential(
        identityToken: ' apple-identity-token ',
        rawNonce: ' raw-nonce-012345678901234567890123 ',
        displayName: ' Apple Learner ',
      );

      expect(requestedBody, {
        'provider': 'APPLE',
        'idToken': 'apple-identity-token',
        'rawNonce': 'raw-nonce-012345678901234567890123',
        'displayName': 'Apple Learner',
      });
    },
  );

  test('loginForReview posts only the entered access code', () async {
    Uri? requestedUri;
    Map<String, Object?>? requestedBody;
    final api = DartIoAuthApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        postJsonTransport: (uri, body, timeout) async {
          requestedUri = uri;
          requestedBody = body;
          return ApiResponse(
            statusCode: 200,
            body: jsonEncode({
              'tokenType': 'Bearer',
              'accessToken': 'review-access.jwt',
              'refreshToken': 'review-refresh.jwt',
              'expiresInSeconds': 1800,
              'user': {'userId': 73, 'name': 'App Review'},
            }),
          );
        },
      ),
    );

    final session = await api.loginForReview(
      ' review-code-with-at-least-32-bytes ',
    );

    expect(
      requestedUri.toString(),
      'http://localhost:8080/api/auth/review/login',
    );
    expect(requestedBody, {'accessCode': 'review-code-with-at-least-32-bytes'});
    expect(session.user.userId, 73);
  });

  test('refreshSession rotates token pair and maps session', () async {
    Uri? requestedUri;
    Map<String, Object?>? requestedBody;
    final api = DartIoAuthApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        postJsonTransport: (uri, body, timeout) async {
          requestedUri = uri;
          requestedBody = body;

          return ApiResponse(
            statusCode: 200,
            body: jsonEncode({
              'tokenType': 'Bearer',
              'accessToken': 'next-access.jwt',
              'refreshToken': 'next-refresh.jwt',
              'expiresInSeconds': 1800,
              'user': {
                'userId': 7,
                'email': 'user@example.com',
                'name': 'LingKo User',
              },
            }),
          );
        },
      ),
    );

    final session = await api.refreshSession('current-refresh.jwt');

    expect(
      requestedUri.toString(),
      'http://localhost:8080/api/auth/token/refresh',
    );
    expect(requestedBody, {'refreshToken': 'current-refresh.jwt'});
    expect(session.accessToken, 'next-access.jwt');
    expect(session.refreshToken, 'next-refresh.jwt');
  });

  test('logout accepts an empty 204 response', () async {
    Uri? requestedUri;
    Map<String, Object?>? requestedBody;
    final api = DartIoAuthApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        postJsonTransport: (uri, body, timeout) async {
          requestedUri = uri;
          requestedBody = body;
          return const ApiResponse(statusCode: 204, body: '');
        },
      ),
    );

    await api.logout('current-refresh.jwt');

    expect(requestedUri.toString(), 'http://localhost:8080/api/auth/logout');
    expect(requestedBody, {'refreshToken': 'current-refresh.jwt'});
  });

  test('deleteAccount sends both access and current refresh tokens', () async {
    Uri? requestedUri;
    Map<String, Object?>? requestedBody;
    Map<String, String>? requestedHeaders;
    final api = DartIoAuthApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        deleteJsonWithHeadersTransport: (uri, body, timeout, headers) async {
          requestedUri = uri;
          requestedBody = body;
          requestedHeaders = headers;
          return const ApiResponse(statusCode: 204, body: '');
        },
      ),
    );

    await api.deleteAccount('access.jwt', 'refresh.jwt');

    expect(requestedUri.toString(), 'http://localhost:8080/api/auth/account');
    expect(requestedBody, {'refreshToken': 'refresh.jwt'});
    expect(requestedHeaders, {'Authorization': 'Bearer access.jwt'});
  });
}
