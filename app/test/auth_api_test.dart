import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/api/api_client.dart';
import 'package:lingko_app/api/auth_api.dart';

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
}
