// 파일 의도: evaluation api test 기능의 사용자·API 계약과 회귀 조건을 검증한다.
// 선택 이유: 네트워크·플랫폼 의존성을 테스트 대역로 통제해 결과를 결정적으로 유지한다.

import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/api/api_client.dart';
import 'package:lingko_app/api/evaluation_api.dart';
import 'package:lingko_app/models/evaluation_job.dart';

void main() {
  test('evaluation uses presigned upload and asynchronous job APIs', () async {
    final audio = File(
      '${Directory.systemTemp.path}/lingko-evaluation-api-${DateTime.now().microsecondsSinceEpoch}.wav',
    );
    await audio.writeAsBytes(List<int>.filled(32044, 0));
    final requestedPosts = <Uri>[];
    Uri? uploadUri;
    String? uploadedPath;
    Map<String, String>? uploadHeaders;
    Duration? uploadTimeout;
    final api = DartIoEvaluationApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        postJsonWithHeadersTransport: (uri, body, timeout, headers) async {
          requestedPosts.add(uri);
          if (uri.path.endsWith('/uploads')) {
            expect(headers, {'Authorization': 'Bearer access.jwt'});
            expect(body['contentType'], 'audio/wav');
            expect(body['contentLength'], 32044);
            return ApiResponse(
              statusCode: 201,
              body: jsonEncode({
                'objectKey': 'evaluation-audio/7/audio-id.wav',
                'uploadUrl': 'https://signed.example/audio',
                'expiresAt': '2026-07-27T01:10:00Z',
              }),
            );
          }
          expect(headers, {
            'Authorization': 'Bearer access.jwt',
            'Idempotency-Key': 'evaluation-request-1',
          });
          expect(body['objectKey'], 'evaluation-audio/7/audio-id.wav');
          expect(body['sentenceId'], 1);
          return ApiResponse(
            statusCode: 202,
            body: jsonEncode({
              'jobId': 'job-id',
              'status': 'PENDING',
              'result': null,
              'errorCode': null,
              'createdAt': '2026-07-27T01:00:00Z',
              'updatedAt': '2026-07-27T01:00:00Z',
            }),
          );
        },
        putFileTransport: (uri, filePath, contentType, timeout) async {
          uploadUri = uri;
          uploadedPath = filePath;
          uploadHeaders = {'Content-Type': contentType};
          uploadTimeout = timeout;
          return const ApiResponse(statusCode: 200, body: '');
        },
      ),
    );

    try {
      final upload = await api.prepareUpload(
        accessToken: 'access.jwt',
        audioPath: audio.path,
      );
      await api.uploadAudio(upload: upload, audioPath: audio.path);
      final job = await api.createJob(
        accessToken: 'access.jwt',
        idempotencyKey: 'evaluation-request-1',
        objectKey: upload.objectKey,
        sentenceId: 1,
      );

      expect(requestedPosts.map((uri) => uri.path), [
        '/api/evaluations/uploads',
        '/api/evaluations/jobs',
      ]);
      expect(uploadUri.toString(), 'https://signed.example/audio');
      expect(uploadedPath, audio.path);
      expect(uploadHeaders, {'Content-Type': 'audio/wav'});
      expect(uploadTimeout, const Duration(seconds: 60));
      expect(job.jobId, 'job-id');
      expect(job.status, EvaluationJobStatus.pending);
    } finally {
      await audio.delete();
    }
  });

  test('completed evaluation job maps the original practice result', () async {
    final api = DartIoEvaluationApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        getJsonTransport: (uri, timeout, headers) async {
          expect(uri.path, '/api/evaluations/jobs/job-id');
          expect(headers, {'Authorization': 'Bearer access.jwt'});
          return ApiResponse(
            statusCode: 200,
            body: jsonEncode({
              'jobId': 'job-id',
              'status': 'SUCCEEDED',
              'errorCode': null,
              'result': {
                'overallScore': 91,
                'gradeLabel': 'Excellent',
                'summary': 'Clear pronunciation.',
                'recognizedText': '안녕하세요.',
                'characterScoreStatus': 'UNAVAILABLE',
                'wordScoreStatus': 'AVAILABLE',
                'scoreBreakdown': {
                  'accuracy': 92,
                  'fluency': 90,
                  'completeness': 93,
                },
                'weakCharacters': [],
                'characters': [],
                'words': [
                  {
                    'position': 0,
                    'text': '안녕하세요',
                    'score': 91,
                    'scoreStatus': 'AVAILABLE',
                    'syllables': [
                      {
                        'position': 0,
                        'text': '안',
                        'score': null,
                        'scoreStatus': 'UNAVAILABLE',
                      },
                    ],
                  },
                ],
              },
              'createdAt': '2026-07-27T01:00:00Z',
              'updatedAt': '2026-07-27T01:00:03Z',
            }),
          );
        },
      ),
    );

    final job = await api.fetchJob(accessToken: 'access.jwt', jobId: 'job-id');

    expect(job.status, EvaluationJobStatus.succeeded);
    expect(job.result?.overallScore, 91);
    expect(job.result?.scoreBreakdown.accuracy, 92);
    expect(job.result?.wordScoreStatus, 'AVAILABLE');
    expect(job.result?.words.single.text, '안녕하세요');
    expect(job.result?.words.single.score, 91);
    expect(job.result?.words.single.syllables.single.scoreStatus, 'UNAVAILABLE');
  });

  test('custom text is sent when creating a job', () async {
    JsonMap? requestedBody;
    final api = DartIoEvaluationApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        postJsonWithHeadersTransport: (uri, body, timeout, headers) async {
          requestedBody = body;
          return ApiResponse(
            statusCode: 202,
            body: jsonEncode({
              'jobId': 'job-id',
              'status': 'PENDING',
              'result': null,
              'errorCode': null,
              'createdAt': '2026-07-27T01:00:00Z',
              'updatedAt': '2026-07-27T01:00:00Z',
            }),
          );
        },
      ),
    );

    await api.createJob(
      accessToken: 'access.jwt',
      idempotencyKey: 'evaluation-request-2',
      objectKey: 'evaluation-audio/7/audio-id.wav',
      text: '안녕하세요.',
    );

    expect(requestedBody, {
      'objectKey': 'evaluation-audio/7/audio-id.wav',
      'text': '안녕하세요',
    });
  });

  test('fallback character scores remain explicitly unavailable', () async {
    final api = DartIoEvaluationApi(
      client: ApiClient(
        baseUrl: 'http://localhost:8080',
        getJsonTransport: (uri, timeout, headers) async {
          return ApiResponse(
            statusCode: 200,
            body: jsonEncode({
              'jobId': 'job-id',
              'status': 'SUCCEEDED',
              'errorCode': null,
              'result': {
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
              },
              'createdAt': '2026-07-27T01:00:00Z',
              'updatedAt': '2026-07-27T01:00:03Z',
            }),
          );
        },
      ),
    );

    final job = await api.fetchJob(accessToken: 'access.jwt', jobId: 'job-id');

    expect(job.result?.characterScoreStatus, 'UNAVAILABLE');
    expect(job.result?.characters.single.scoreStatus, 'UNAVAILABLE');
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
                  'characters': [
                    {
                      'position': 0,
                      'text': '맛',
                      'score': 88,
                      'feedback': 'Keep the final consonant clear.',
                      'mouthGuideUrl': 'https://guides/mouth/mat.mp4',
                      'tongueGuideUrl': null,
                    },
                  ],
                  'words': [
                    {
                      'position': 0,
                      'text': '마싯게따',
                      'score': 88,
                      'scoreStatus': 'AVAILABLE',
                      'syllables': [
                        {
                          'position': 0,
                          'text': '맛',
                          'score': null,
                          'feedback': 'Keep the final consonant clear.',
                          'mouthGuideUrl': 'https://guides/mouth/mat.mp4',
                          'tongueGuideUrl': null,
                        },
                      ],
                    },
                  ],
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
    expect(history.items.single.characters.single.character, '맛');
    expect(history.items.single.characters.single.score, 88);
    expect(history.items.single.characters.single.scoreStatus, 'AVAILABLE');
    expect(history.items.single.words.single.text, '마싯게따');
    expect(history.items.single.words.single.score, 88);
    expect(history.items.single.words.single.syllables.single.scoreStatus, 'UNAVAILABLE');
    expect(
      history.items.single.characters.single.note,
      'Keep the final consonant clear.',
    );
  });
}
