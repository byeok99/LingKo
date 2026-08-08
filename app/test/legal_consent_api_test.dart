// 파일 의도: 법적 동의 API의 인증 헤더·요청 body·응답 매핑 계약을 검증한다.

import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/api/api_client.dart';
import 'package:lingko_app/api/legal_consent_api.dart';
import 'package:lingko_app/models/consent_selection.dart';

void main() {
  test('fetchStatus sends bearer token and maps current requirement', () async {
    Uri? requestedUri;
    Map<String, String>? requestedHeaders;
    final api = DartIoLegalConsentApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        getJsonTransport: (uri, timeout, headers) async {
          requestedUri = uri;
          requestedHeaders = headers;
          return ApiResponse(
            statusCode: 200,
            body: jsonEncode({
              'required': true,
              'documentVersion': '2026-08-07',
            }),
          );
        },
      ),
    );

    final status = await api.fetchStatus(accessToken: 'access.jwt');

    expect(requestedUri.toString(), 'http://localhost:8080/api/legal/consent');
    expect(requestedHeaders, {'Authorization': 'Bearer access.jwt'});
    expect(status.required, isTrue);
    expect(status.documentVersion, '2026-08-07');
  });

  test(
    'record sends authenticated selection without a user-controlled id',
    () async {
      Map<String, Object?>? requestedBody;
      Map<String, String>? requestedHeaders;
      final api = DartIoLegalConsentApi(
        client: ApiClient(
          baseUrl: 'http://localhost:8080',
          postJsonWithHeadersTransport: (uri, body, timeout, headers) async {
            requestedBody = body;
            requestedHeaders = headers;
            return ApiResponse(
              statusCode: 200,
              body: jsonEncode({
                'required': false,
                'documentVersion': '2026-08-07',
              }),
            );
          },
        ),
      );
      final selection = ConsentSelection(
        termsAgreed: true,
        privacyAcknowledged: true,
        marketingOptIn: false,
        documentVersion: '2026-08-07',
        agreedAt: DateTime.utc(2026, 8, 7, 1, 2, 3),
      );

      final status = await api.record(
        accessToken: 'access.jwt',
        selection: selection,
      );

      expect(requestedHeaders, {'Authorization': 'Bearer access.jwt'});
      expect(requestedBody, selection.toJson());
      expect(requestedBody, isNot(contains('userId')));
      expect(status.required, isFalse);
    },
  );
}
