// 파일 의도: practice 할당량 api test 기능의 사용자·API 계약과 회귀 조건을 검증한다.
// 선택 이유: 네트워크·플랫폼 의존성을 테스트 대역로 통제해 결과를 결정적으로 유지한다.

import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/api/api_client.dart';
import 'package:lingko_app/api/practice_quota_api.dart';

void main() {
  test('fetchTodayQuota sends bearer token and maps response', () async {
    Uri? requestedUri;
    Map<String, String>? requestedHeaders;
    final api = DartIoPracticeQuotaApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        getJsonTransport: (uri, timeout, headers) async {
          requestedUri = uri;
          requestedHeaders = headers;

          return ApiResponse(
            statusCode: 200,
            body: jsonEncode({
              'date': '2026-06-17',
              'freeLimit': 5,
              'freeUsed': 2,
              'rewardedAvailable': 1,
              'remainingPractices': 4,
              'nextRefillAt': '2026-06-17T13:00:00+09:00',
              'serverTime': '2026-06-17T12:17:42+09:00',
            }),
          );
        },
      ),
    );

    final quota = await api.fetchTodayQuota(accessToken: 'access.jwt');

    expect(requestedUri.toString(), 'http://localhost:8080/api/quota/today');
    expect(requestedHeaders, {'Authorization': 'Bearer access.jwt'});
    expect(quota.freeLimit, 5);
    expect(quota.freeUsed, 2);
    expect(quota.rewardedAvailable, 1);
    expect(quota.remainingPractices, 4);
    expect(quota.nextRefillAt, DateTime.parse('2026-06-17T13:00:00+09:00'));
    expect(quota.serverTime, DateTime.parse('2026-06-17T12:17:42+09:00'));
  });
}
