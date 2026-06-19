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
}
