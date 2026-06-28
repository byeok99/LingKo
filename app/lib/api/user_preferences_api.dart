import '../models/user_preferences.dart';
import 'api_client.dart';

abstract class UserPreferencesApi {
  Future<UserPreferences> fetchPreferences({required String accessToken});

  Future<UserPreferences> updatePreferences({
    required String accessToken,
    required UserPreferences preferences,
  });
}

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
