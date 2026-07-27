// 파일 의도: S3 직접 업로드와 비동기 평가 작업 상태의 앱 내부 계약을 정의한다.

import 'practice_result.dart';

enum EvaluationJobStatus {
  pending,
  processing,
  succeeded,
  failed;

  factory EvaluationJobStatus.fromJson(Object? value) {
    return switch (value) {
      'PENDING' => pending,
      'PROCESSING' => processing,
      'SUCCEEDED' => succeeded,
      'FAILED' => failed,
      _ => throw const FormatException('Invalid evaluation job status'),
    };
  }
}

/// 제한 시간 S3 PUT URL과 서버가 소유권 검증에 사용하는 object key를 묶는다.
class EvaluationUpload {
  const EvaluationUpload({
    required this.objectKey,
    required this.uploadUrl,
    required this.expiresAt,
  });

  factory EvaluationUpload.fromJson(Map<String, Object?> json) {
    return EvaluationUpload(
      objectKey: _requiredString(json['objectKey']),
      uploadUrl: _requiredString(json['uploadUrl']),
      expiresAt: DateTime.parse(_requiredString(json['expiresAt'])),
    );
  }

  final String objectKey;
  final String uploadUrl;
  final DateTime expiresAt;
}

/// Polling 가능한 평가 작업 상태와 완료 결과 또는 실패 코드를 나타낸다.
class EvaluationJob {
  const EvaluationJob({
    required this.jobId,
    required this.status,
    this.result,
    this.errorCode,
  });

  factory EvaluationJob.fromJson(Map<String, Object?> json) {
    final result = json['result'];
    return EvaluationJob(
      jobId: _requiredString(json['jobId']),
      status: EvaluationJobStatus.fromJson(json['status']),
      result:
          result is Map<String, Object?>
              ? PracticeResult.fromJson(result)
              : null,
      errorCode:
          json['errorCode'] is String ? json['errorCode'] as String : null,
    );
  }

  final String jobId;
  final EvaluationJobStatus status;
  final PracticeResult? result;
  final String? errorCode;
}

String _requiredString(Object? value) {
  if (value is String && value.isNotEmpty) {
    return value;
  }
  throw const FormatException('Required response field is missing');
}
