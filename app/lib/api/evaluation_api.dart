// 파일 의도: evaluation api 백엔드 통신 경계를 정의한다.
// 선택 이유: HTTP 전송과 JSON 매핑을 UI에서 분리해 API 변경 영향을 한곳에서 관리한다.

import 'dart:io';

import '../models/evaluation_job.dart';
import '../models/practice_history.dart';
import 'api_client.dart';

/// Evaluation Api 백엔드 통신 계약을 정의한다.
/// 화면과 서비스가 HTTP 구현이 아닌 추상 계약에 의존하도록 인터페이스 역할의 추상 클래스를 선택했다.
abstract class EvaluationApi {
  Future<EvaluationUpload> prepareUpload({
    required String accessToken,
    required String audioPath,
  });

  Future<void> uploadAudio({
    required EvaluationUpload upload,
    required String audioPath,
  });

  Future<EvaluationJob> createJob({
    required String accessToken,
    required String idempotencyKey,
    required String objectKey,
    int? sentenceId,
    String? text,
  });

  Future<EvaluationJob> fetchJob({
    required String accessToken,
    required String jobId,
  });

  Future<PracticeHistory> fetchHistory({
    required String accessToken,
    int page = 0,
    int size = 10,
  });
}

/// Dart Io Evaluation Api 백엔드 요청·응답 매핑을 구현한다.
/// 전송 실패와 JSON 형식 오류를 API 경계에서 정규화해 UI에는 형식이 지정된 결과만 전달한다.
class DartIoEvaluationApi implements EvaluationApi {
  DartIoEvaluationApi({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  @override
  Future<EvaluationUpload> prepareUpload({
    required String accessToken,
    required String audioPath,
  }) async {
    final file = File(audioPath);
    final json = await _client.postJsonWithHeaders(
      '/api/evaluations/uploads',
      {
        'fileName': _basename(audioPath),
        'contentType': 'audio/wav',
        'contentLength': await file.length(),
      },
      {'Authorization': 'Bearer ${accessToken.trim()}'},
    );
    return EvaluationUpload.fromJson(json);
  }

  @override
  Future<void> uploadAudio({
    required EvaluationUpload upload,
    required String audioPath,
  }) {
    return _client.putFile(
      url: upload.uploadUrl,
      filePath: audioPath,
      contentType: 'audio/wav',
    );
  }

  @override
  Future<EvaluationJob> createJob({
    required String accessToken,
    required String idempotencyKey,
    required String objectKey,
    int? sentenceId,
    String? text,
  }) async {
    final json = await _client.postJsonWithHeaders(
      '/api/evaluations/jobs',
      {
        'objectKey': objectKey,
        if (sentenceId != null) 'sentenceId': sentenceId,
        if (text != null && text.trim().isNotEmpty) 'text': text.trim(),
      },
      {
        'Authorization': 'Bearer ${accessToken.trim()}',
        'Idempotency-Key': idempotencyKey,
      },
    );
    return EvaluationJob.fromJson(json);
  }

  @override
  Future<EvaluationJob> fetchJob({
    required String accessToken,
    required String jobId,
  }) async {
    final json = await _client.getJson(
      '/api/evaluations/jobs/$jobId',
      const {},
      {'Authorization': 'Bearer ${accessToken.trim()}'},
    );
    return EvaluationJob.fromJson(json);
  }

  @override
  Future<PracticeHistory> fetchHistory({
    required String accessToken,
    int page = 0,
    int size = 10,
  }) async {
    final json = await _client.getJson(
      '/api/evaluations/me',
      {'page': page, 'size': size},
      {'Authorization': 'Bearer ${accessToken.trim()}'},
    );

    return PracticeHistory.fromJson(json);
  }
}

String _basename(String path) {
  return path.split(RegExp(r'[/\\]')).last;
}
