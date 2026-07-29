// 파일 의도: auth api 백엔드 통신 경계를 정의한다.
// 선택 이유: HTTP 전송과 JSON 매핑을 UI에서 분리해 API 변경 영향을 한곳에서 관리한다.

import '../models/auth_session.dart';
import 'api_client.dart';

/// 앱 세션을 생성·회전·폐기하는 백엔드 연산을 정의한다.
abstract class AuthApi {
  /// Google ID 토큰을 최초 LingKo 토큰 쌍으로 교환한다.
  Future<AuthSession> loginWithGoogleIdToken(String idToken);

  /// 현재 갱신 토큰을 회전된 토큰 쌍으로 교체한다.
  Future<AuthSession> refreshSession(String refreshToken);

  /// [refreshToken]이 나타내는 기기 세션을 폐기한다.
  Future<void> logout(String refreshToken);

  /// 현재 두 토큰을 재확인하고 계정과 사용자 소유 데이터를 삭제한다.
  Future<void> deleteAccount(String accessToken, String refreshToken);
}

/// 앱의 Dart IO JSON client로 [AuthApi]를 구현한다.
class DartIoAuthApi implements AuthApi {
  DartIoAuthApi({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  @override
  Future<AuthSession> loginWithGoogleIdToken(String idToken) async {
    final json = await _client.postJson('/api/auth/oauth/login', {
      'provider': 'GOOGLE',
      'idToken': idToken.trim(),
    });

    return AuthSession.fromJson(json);
  }

  @override
  Future<AuthSession> refreshSession(String refreshToken) async {
    final json = await _client.postJson('/api/auth/token/refresh', {
      'refreshToken': refreshToken.trim(),
    });

    return AuthSession.fromJson(json);
  }

  @override
  Future<void> logout(String refreshToken) {
    return _client.postJsonWithoutResponse('/api/auth/logout', {
      'refreshToken': refreshToken.trim(),
    });
  }

  @override
  Future<void> deleteAccount(String accessToken, String refreshToken) {
    return _client.deleteJsonWithoutResponse(
      '/api/auth/account',
      {'refreshToken': refreshToken.trim()},
      {'Authorization': 'Bearer ${accessToken.trim()}'},
    );
  }
}
