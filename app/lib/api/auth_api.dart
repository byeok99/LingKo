import '../models/auth_session.dart';
import 'api_client.dart';

abstract class AuthApi {
  Future<AuthSession> loginWithGoogleIdToken(String idToken);

  Future<AuthSession> refreshSession(String refreshToken);

  Future<void> logout(String refreshToken);
}

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
}
