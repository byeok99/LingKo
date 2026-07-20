import '../models/practice_quota.dart';
import 'api_client.dart';

abstract class PracticeQuotaApi {
  Future<PracticeQuota> fetchTodayQuota({required String accessToken});
}

class DartIoPracticeQuotaApi implements PracticeQuotaApi {
  DartIoPracticeQuotaApi({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  @override
  Future<PracticeQuota> fetchTodayQuota({required String accessToken}) async {
    final json = await _client.getJson('/api/quota/today', const {}, {
      'Authorization': 'Bearer ${accessToken.trim()}',
    });

    return PracticeQuota.fromJson(json);
  }
}
