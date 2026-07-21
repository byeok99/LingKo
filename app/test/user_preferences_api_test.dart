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
            body: jsonEncode({
              'displayLanguage': 'ko',
              'nativeLanguage': 'en',
              'targetLevel': 'INTERMEDIATE_1',
            }),
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
    expect(preferences.displayLanguage, 'ko');
    expect(preferences.nativeLanguage, 'en');
    expect(preferences.targetLevel, LearningLevel.intermediate1);
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
            body: jsonEncode({
              'displayLanguage': 'ko',
              'nativeLanguage': 'ja',
              'targetLevel': 'BEGINNER_2',
            }),
          );
        },
      ),
    );

    final preferences = await api.updatePreferences(
      accessToken: 'access.jwt',
      preferences: const UserPreferences(
        displayLanguage: 'ko',
        nativeLanguage: 'ja',
        targetLevel: LearningLevel.beginner2,
      ),
    );

    expect(
      requestedUri.toString(),
      'http://localhost:8080/api/users/me/preferences',
    );
    expect(requestedHeaders, {'Authorization': 'Bearer access.jwt'});
    expect(requestedBody, {
      'displayLanguage': 'ko',
      'nativeLanguage': 'ja',
      'targetLevel': 'BEGINNER_2',
    });
    expect(preferences.nativeLanguage, 'ja');
  });
}
