// 파일 의도: evaluation api test 기능의 사용자·API 계약과 회귀 조건을 검증한다.
// 선택 이유: 네트워크·플랫폼 의존성을 테스트 대역로 통제해 결과를 결정적으로 유지한다.

import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/api/api_client.dart';
import 'package:lingko_app/api/evaluation_api.dart';

void main() {
  test('evaluate uploads audio with sentenceId and maps result', () async {
    Uri? requestedUri;
    MultipartUpload? requestedUpload;
    final api = DartIoEvaluationApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        multipartTransport: (uri, upload, timeout) async {
          requestedUri = uri;
          requestedUpload = upload;

          return ApiResponse(
            statusCode: 200,
            body: jsonEncode({
              'overallScore': 91,
              'gradeLabel': 'Excellent',
              'summary': 'Clear pronunciation.',
              'recognizedText': '안녕하세요.',
              'characterScoreStatus': 'UNAVAILABLE',
              'scoreBreakdown': {
                'accuracy': 92,
                'fluency': 90,
                'completeness': 93,
              },
              'weakCharacters': [],
              'characters': [],
            }),
          );
        },
      ),
    );

    final result = await api.evaluate(
      audioPath: '/tmp/recording.wav',
      sentenceId: 1,
    );

    expect(requestedUri.toString(), 'http://localhost:8080/api/evaluations');
    expect(requestedUpload?.file.fieldName, 'audio');
    expect(requestedUpload?.file.filename, 'recording.wav');
    expect(requestedUpload?.file.contentType, 'audio/wav');
    expect(requestedUpload?.fields, {'sentenceId': '1'});
    expect(result.overallScore, 91);
    expect(result.scoreBreakdown.accuracy, 92);
    expect(result.recognizedText, '안녕하세요.');
    expect(result.characterScoreStatus, 'UNAVAILABLE');
  });

  test('evaluate uploads audio with custom text', () async {
    MultipartUpload? requestedUpload;
    final api = DartIoEvaluationApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        multipartTransport: (uri, upload, timeout) async {
          requestedUpload = upload;

          return ApiResponse(
            statusCode: 200,
            body: jsonEncode({
              'overallScore': 80,
              'gradeLabel': 'Good',
              'summary': 'Good pronunciation.',
              'scoreBreakdown': {
                'accuracy': 80,
                'fluency': 80,
                'completeness': 80,
              },
              'weakCharacters': [],
              'characters': [],
            }),
          );
        },
      ),
    );

    await api.evaluate(audioPath: r'C:\tmp\recording.wav', text: '안녕하세요.');

    expect(requestedUpload?.file.filename, 'recording.wav');
    expect(requestedUpload?.fields, {'text': '안녕하세요.'});
  });

  test('fallback character scores remain explicitly unavailable', () async {
    final api = DartIoEvaluationApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        multipartTransport: (uri, upload, timeout) async {
          return ApiResponse(
            statusCode: 200,
            body: jsonEncode({
              'overallScore': 80,
              'gradeLabel': 'Good',
              'summary': 'Good pronunciation.',
              'recognizedText': '가나',
              'characterScoreStatus': 'UNAVAILABLE',
              'scoreBreakdown': {
                'accuracy': 80,
                'fluency': 80,
                'completeness': 80,
              },
              'weakCharacters': [],
              'characters': [
                {
                  'pronunciationText': '가',
                  'score': null,
                  'scoreStatus': 'UNAVAILABLE',
                  'note': 'Focus on placement',
                  'guideType': 'MOUTH',
                },
              ],
            }),
          );
        },
      ),
    );

    final result = await api.evaluate(
      audioPath: '/tmp/recording.wav',
      text: '가나',
    );

    expect(result.characterScoreStatus, 'UNAVAILABLE');
    expect(result.characters.single.scoreStatus, 'UNAVAILABLE');
  });

  test('fetchHistory requests my evaluations and maps response', () async {
    Uri? requestedUri;
    Map<String, String>? requestedHeaders;
    final api = DartIoEvaluationApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        getJsonTransport: (uri, timeout, headers) async {
          requestedUri = uri;
          requestedHeaders = headers;

          return ApiResponse(
            statusCode: 200,
            body: jsonEncode({
              'items': [
                {
                  'evaluationLogId': 10,
                  'sentenceId': 1,
                  'source': 'RECOMMENDED',
                  'originalText': '맛있겠다.',
                  'standardPronunciation': '마싯게따.',
                  'recognizedText': '마싯게따.',
                  'overallScore': 91,
                  'gradeLabel': 'Excellent',
                  'summary': 'Clear pronunciation.',
                  'scoreBreakdown': {
                    'accuracy': 92,
                    'fluency': 90,
                    'completeness': 93,
                  },
                  'characters': [],
                  'createdAt': '2026-06-26T09:30:00',
                },
              ],
              'page': 0,
              'size': 2,
              'totalItems': 1,
              'totalPages': 1,
              'hasNext': false,
              'bestScore': 91,
            }),
          );
        },
      ),
    );

    final history = await api.fetchHistory(
      accessToken: 'access.jwt',
      page: 0,
      size: 2,
    );

    expect(
      requestedUri.toString(),
      'http://localhost:8080/api/evaluations/me?page=0&size=2',
    );
    expect(requestedHeaders, {'Authorization': 'Bearer access.jwt'});
    expect(history.bestScore, 91);
    expect(history.items.single.originalText, '맛있겠다.');
    expect(history.items.single.scoreBreakdown.accuracy, 92);
  });
}
