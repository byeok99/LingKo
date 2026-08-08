// 파일 의도: 인증 사용자의 약관 동의 상태 조회·제출 HTTP 계약을 캡슐화한다.

import '../models/consent_selection.dart';
import '../models/legal_consent_status.dart';
import 'api_client.dart';

/// 약관 동의 상태와 기록을 제공하는 백엔드 경계다.
abstract class LegalConsentApi {
  Future<LegalConsentStatus> fetchStatus({required String accessToken});

  Future<LegalConsentStatus> record({
    required String accessToken,
    required ConsentSelection selection,
  });
}

/// Bearer token으로 사용자를 식별해 동의 body가 다른 사용자 ID를 지정하지 못하게 한다.
class DartIoLegalConsentApi implements LegalConsentApi {
  DartIoLegalConsentApi({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  @override
  Future<LegalConsentStatus> fetchStatus({required String accessToken}) async {
    final json = await _client.getJson('/api/legal/consent', const {}, {
      'Authorization': 'Bearer ${accessToken.trim()}',
    });
    return LegalConsentStatus.fromJson(json);
  }

  @override
  Future<LegalConsentStatus> record({
    required String accessToken,
    required ConsentSelection selection,
  }) async {
    final json = await _client.postJsonWithHeaders(
      '/api/legal/consent',
      selection.toJson(),
      {'Authorization': 'Bearer ${accessToken.trim()}'},
    );
    return LegalConsentStatus.fromJson(json);
  }
}
