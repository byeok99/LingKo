// 파일 의도: user preferences api test 기능의 사용자·API 계약과 회귀 조건을 검증한다.
// 선택 이유: 네트워크·플랫폼 의존성을 테스트 대역로 통제해 결과를 결정적으로 유지한다.

import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/api/api_client.dart';
import 'package:lingko_app/api/user_preferences_api.dart';
import 'package:lingko_app/models/user_preferences.dart';

void main() {
  test('fetchPreferences sends bearer token and maps response', () async {
    Uri? requestedUri;
    Map<String, String>? requestedHeaders;
    final api = DartIoUserPreferencesApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        getJsonTransport: (uri, timeout, headers) async {
          requestedUri = uri;
          requestedHeaders = headers;

          return ApiResponse(
            statusCode: 200,
            body: jsonEncode({'nativeLanguage': 'en'}),
          );
        },
      ),
    );

    final preferences = await api.fetchPreferences(accessToken: 'access.jwt');

    expect(
      requestedUri.toString(),
      'http://localhost:8080/api/users/me/preferences',
    );
    expect(requestedHeaders, {'Authorization': 'Bearer access.jwt'});

    expect(preferences.nativeLanguage, 'en');
  });

  test('updatePreferences sends bearer token and request body', () async {
    Uri? requestedUri;
    Map<String, Object?>? requestedBody;
    Map<String, String>? requestedHeaders;
    final api = DartIoUserPreferencesApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        patchJsonTransport: (uri, body, timeout, headers) async {
          requestedUri = uri;
          requestedBody = body;
          requestedHeaders = headers;

          return ApiResponse(
            statusCode: 200,
            body: jsonEncode({'nativeLanguage': 'ja'}),
          );
        },
      ),
    );

    final preferences = await api.updatePreferences(
      accessToken: 'access.jwt',
      preferences: const UserPreferences(
        nativeLanguage: 'ja',
      ),
    );

    expect(
      requestedUri.toString(),
      'http://localhost:8080/api/users/me/preferences',
    );
    expect(requestedHeaders, {'Authorization': 'Bearer access.jwt'});
    expect(requestedBody, {'nativeLanguage': 'ja'});
    expect(preferences.nativeLanguage, 'ja');
  });
}
