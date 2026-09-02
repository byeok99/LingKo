// 파일 의도: S3 직접 업로드와 비동기 평가 작업 상태의 앱 내부 계약을 정의한다.

import 'practice_result.dart';

/// Backend 평가 작업의 polling 상태이며 알 수 없는 wire 값은 허용하지 않는다.
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

/// Worker가 완료율을 추측하지 않고 실제로 진입한 처리 경계를 전달하는 wire 상태다.
enum EvaluationJobPhase {
  queued,
  downloadingAudio,
  analyzingSpeech,
  preparingGuides,
  finalizing;

  /// 알 수 없는 값은 거부하되 순차 배포 중 누락된 값만 기존 status로 안전하게 보완한다.
  factory EvaluationJobPhase.fromJson(
    Object? value, {
    required EvaluationJobStatus status,
  }) {
    return switch (value) {
      'QUEUED' => queued,
      'DOWNLOADING_AUDIO' => downloadingAudio,
      'ANALYZING_SPEECH' => analyzingSpeech,
      'PREPARING_GUIDES' => preparingGuides,
      'FINALIZING' => finalizing,
      // Backend 순차 배포 중 phase가 없는 구버전 응답은 status에 맞춘 보수적인 단계로 해석한다.
      null => switch (status) {
        EvaluationJobStatus.pending => queued,
        EvaluationJobStatus.processing => analyzingSpeech,
        EvaluationJobStatus.succeeded ||
        EvaluationJobStatus.failed => finalizing,
      },
      _ => throw const FormatException('Invalid evaluation job phase'),
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
    this.phase = EvaluationJobPhase.queued,
    this.result,
    this.errorCode,
  });

  factory EvaluationJob.fromJson(Map<String, Object?> json) {
    final result = json['result'];
    final status = EvaluationJobStatus.fromJson(json['status']);
    return EvaluationJob(
      jobId: _requiredString(json['jobId']),
      status: status,
      phase: EvaluationJobPhase.fromJson(json['phase'], status: status),
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

  /// 완료율이 아니라 Worker가 마지막으로 진입한 실제 처리 경계다.
  final EvaluationJobPhase phase;
  final PracticeResult? result;
  final String? errorCode;
}

String _requiredString(Object? value) {
  if (value is String && value.isNotEmpty) {
    return value;
  }
  throw const FormatException('Required response field is missing');
}
