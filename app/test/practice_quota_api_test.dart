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
              'resetAt': '2026-06-18T00:00:00+09:00',
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
    expect(quota.resetAt, DateTime.parse('2026-06-18T00:00:00+09:00'));
  });
}
