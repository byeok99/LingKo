// 파일 의도: user preferences api 백엔드 통신 경계를 정의한다.
// 선택 이유: HTTP 전송과 JSON 매핑을 UI에서 분리해 API 변경 영향을 한곳에서 관리한다.

import '../models/user_preferences.dart';
import 'api_client.dart';

/// User Preferences Api 백엔드 통신 계약을 정의한다.
/// 화면과 서비스가 HTTP 구현이 아닌 추상 계약에 의존하도록 인터페이스 역할의 추상 클래스를 선택했다.
abstract class UserPreferencesApi {
  Future<UserPreferences> fetchPreferences({required String accessToken});

  Future<UserPreferences> updatePreferences({
    required String accessToken,
    required UserPreferences preferences,
  });
}

/// Dart Io User Preferences Api 백엔드 요청·응답 매핑을 구현한다.
/// 전송 실패와 JSON 형식 오류를 API 경계에서 정규화해 UI에는 형식이 지정된 결과만 전달한다.
class DartIoUserPreferencesApi implements UserPreferencesApi {
  DartIoUserPreferencesApi({ApiClient? client})
    : _client = client ?? ApiClient();

  final ApiClient _client;

  @override
  Future<UserPreferences> fetchPreferences({
    required String accessToken,
  }) async {
    final json = await _client.getJson('/api/users/me/preferences', const {}, {
      'Authorization': 'Bearer ${accessToken.trim()}',
    });

    return UserPreferences.fromJson(json);
  }

  @override
  Future<UserPreferences> updatePreferences({
    required String accessToken,
    required UserPreferences preferences,
  }) async {
    final json = await _client.patchJson(
      '/api/users/me/preferences',
      preferences.toJson(),
      {'Authorization': 'Bearer ${accessToken.trim()}'},
    );

    return UserPreferences.fromJson(json);
  }
}
