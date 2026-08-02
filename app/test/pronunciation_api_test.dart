// 파일 의도: pronunciation api test 기능의 사용자·API 계약과 회귀 조건을 검증한다.
// 선택 이유: 네트워크·플랫폼 의존성을 테스트 대역로 통제해 결과를 결정적으로 유지한다.

import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/api/api_client.dart';
import 'package:lingko_app/api/pronunciation_api.dart';

void main() {
  test('ApiClient defaults to Android emulator host on Android', () {
    expect(resolveLingKoApiBaseUrl(isAndroid: true), 'http://10.0.2.2:8080');
  });

  test('ApiClient defaults to localhost outside Android emulator', () {
    expect(resolveLingKoApiBaseUrl(isAndroid: false), 'http://localhost:8080');
  });

  test('ApiClient uses LINGKO_API_BASE_URL override when provided', () {
    expect(
      resolveLingKoApiBaseUrl(
        environmentOverride: ' http://192.168.0.10:8080 ',
        isAndroid: true,
      ),
      'http://192.168.0.10:8080',
    );
  });

  test(
    'prepareCustomSentence posts CUSTOM request and maps response',
    () async {
      Uri? requestedUri;
      Map<String, Object?>? requestedBody;
      final api = DartIoPronunciationApi(
        client: ApiClient(
          baseUrl: 'http://localhost:8080',
          postJsonTransport: (uri, body, timeout) async {
            requestedUri = uri;
            requestedBody = body;

            return ApiResponse(
              statusCode: 200,
              body: jsonEncode({
                'sentence': {
                  'source': 'CUSTOM',
                  'originalText': '한국어를 배우고 있어요.',
                  'standardPronunciation': '한구거를 배우고 이써요.',
                  'translation': 'Practice with your own sentence.',
                  'categoryLabel': 'Free practice',
                  'learningPoint': 'Linking across syllables',
                  'initialScore': 0,
                  'characters': [
                    {
                      'position': 0,
                      'text': '한',
                      'pronunciationText': '한',
                      'phonemes': ['ㅎ', 'ㅏ', 'ㄴ'],
                      'guideType': 'TONGUE',
                      'guideStatus': 'AVAILABLE',
                      'mouthGuideUrl': null,
                      'tongueGuideUrl': 'https://guides/tongue/h.png',
                      'note': 'Focus on tongue placement',
                    },
                  ],
                },
              }),
            );
          },
        ),
      );

      final sentence = await api.prepareCustomSentence('한국어를 배우고 있어요.!?');

      expect(
        requestedUri.toString(),
        'http://localhost:8080/api/pronunciation/prepare',
      );
      expect(requestedBody, {'source': 'CUSTOM', 'text': '한국어를 배우고 있어요'});
      expect(sentence.text, '한국어를 배우고 있어요');
      expect(sentence.pronunciation, '한구거를 배우고 이써요');
      expect(sentence.characters.single.kind, 'TONGUE');
      expect(
        sentence.characters.single.tongueGuideUrl,
        'https://guides/tongue/h.png',
      );
    },
  );

  test('ApiClient maps server error message', () async {
    final client = ApiClient(
      baseUrl: 'http://localhost:8080',
      postJsonTransport: (uri, body, timeout) async {
        return ApiResponse(
          statusCode: 400,
          body: jsonEncode({
            'code': 'VALIDATION_FAILED',
            'message': 'Validation failed',
          }),
        );
      },
    );

    expect(
      () => client.postJson('/api/pronunciation/prepare', {'source': 'CUSTOM'}),
      throwsA(
        isA<ApiException>().having(
          (exception) => exception.message,
          'message',
          'Validation failed',
        ),
      ),
    );
  });

  test('ApiClient maps non-JSON error body to stable message', () async {
    final client = ApiClient(
      baseUrl: 'http://localhost:8080',
      postJsonTransport: (uri, body, timeout) async {
        return const ApiResponse(
          statusCode: 502,
          body: '<html>Bad Gateway</html>',
        );
      },
    );

    expect(
      () => client.postJson('/api/pronunciation/prepare', {'source': 'CUSTOM'}),
      throwsA(
        isA<ApiException>()
            .having(
              (exception) => exception.message,
              'message',
              'Request failed with status 502',
            )
            .having((exception) => exception.statusCode, 'statusCode', 502),
      ),
    );
  });

  test('ApiClient maps empty error body to stable message', () async {
    final client = ApiClient(
      baseUrl: 'http://localhost:8080',
      postJsonTransport: (uri, body, timeout) async {
        return const ApiResponse(statusCode: 500, body: '');
      },
    );

    expect(
      () => client.postJson('/api/pronunciation/prepare', {'source': 'CUSTOM'}),
      throwsA(
        isA<ApiException>()
            .having(
              (exception) => exception.message,
              'message',
              'Request failed with status 500',
            )
            .having((exception) => exception.statusCode, 'statusCode', 500),
      ),
    );
  });
}
