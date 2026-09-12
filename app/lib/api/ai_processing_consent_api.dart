import '../models/ai_processing_consent.dart';
import 'api_client.dart';

/// 계정별 AI 전송 동의를 조회·기록한다. 로컬 preference를 권한으로 사용하지 않는다.
abstract class AiProcessingConsentApi {
  Future<AiProcessingConsent> fetchStatus({required String accessToken});
  Future<AiProcessingConsent> record({
    required String accessToken,
    required bool granted,
  });
}

/// 서버가 인증한 계정에만 동의를 기록하는 HTTP adapter다.
class DartIoAiProcessingConsentApi implements AiProcessingConsentApi {
  DartIoAiProcessingConsentApi({ApiClient? client})
    : _client = client ?? ApiClient();
  final ApiClient _client;

  @override
  Future<AiProcessingConsent> fetchStatus({required String accessToken}) async {
    return AiProcessingConsent.fromJson(
      await _client.getJson('/api/legal/ai-consent', const {}, {
        'Authorization': 'Bearer $accessToken',
      }),
    );
  }

  @override
  Future<AiProcessingConsent> record({
    required String accessToken,
    required bool granted,
  }) async {
    return AiProcessingConsent.fromJson(
      await _client.postJsonWithHeaders(
        '/api/legal/ai-consent',
        {
          'granted': granted,
          'noticeVersion': AiProcessingConsent.supportedVersion,
        },
        {'Authorization': 'Bearer $accessToken'},
      ),
    );
  }
}
