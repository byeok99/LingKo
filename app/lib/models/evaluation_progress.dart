// 파일 의도: 비동기 평가 작업의 실제 진행 단계를 앱 shell과 여러 화면이 공유한다.

import 'evaluation_job.dart';

/// 서버 작업과 클라이언트 업로드 경계를 가짜 퍼센트 없이 표현한다.
enum EvaluationProgressStage {
  idle,
  uploading,
  creatingJob,
  queued,
  downloadingAudio,
  analyzing,
  preparingFeedback,
  finalizing,
  completed,
  failed,
}

/// 탭 이동 후에도 평가 작업 식별자와 현재 단계를 잃지 않도록 하는 불변 상태다.
class EvaluationProgress {
  const EvaluationProgress({
    this.stage = EvaluationProgressStage.idle,
    this.jobId,
    this.message,
    this.failedAt,
    this.uploadFraction,
  });

  /// Backend 작업 상태를 사용자가 이해할 수 있는 실제 처리 단계와 안내로 변환한다.
  factory EvaluationProgress.fromJob(EvaluationJob job) {
    if (job.status == EvaluationJobStatus.succeeded) {
      return EvaluationProgress(
        stage: EvaluationProgressStage.completed,
        jobId: job.jobId,
        message: 'Your pronunciation result is ready.',
      );
    }
    if (job.status == EvaluationJobStatus.failed) {
      return EvaluationProgress(
        stage: EvaluationProgressStage.failed,
        jobId: job.jobId,
        failedAt: EvaluationProgressStage.analyzing,
        message: 'The evaluation did not finish.',
      );
    }

    final (stage, message) = switch (job.phase) {
      EvaluationJobPhase.queued => (
        EvaluationProgressStage.queued,
        'Your evaluation is waiting to start.',
      ),
      EvaluationJobPhase.downloadingAudio => (
        EvaluationProgressStage.downloadingAudio,
        'Securely loading your recording.',
      ),
      EvaluationJobPhase.analyzingSpeech => (
        EvaluationProgressStage.analyzing,
        'Checking accuracy, fluency, and completeness.',
      ),
      EvaluationJobPhase.preparingGuides => (
        EvaluationProgressStage.preparingFeedback,
        'Building your personalized feedback.',
      ),
      EvaluationJobPhase.finalizing => (
        EvaluationProgressStage.finalizing,
        'Finishing your result.',
      ),
    };
    return EvaluationProgress(stage: stage, jobId: job.jobId, message: message);
  }

  final EvaluationProgressStage stage;
  final String? jobId;
  final String? message;
  final EvaluationProgressStage? failedAt;

  /// 업로드한 byte/전체 byte로 계산한 0~1 값이며 서버 처리 단계에서는 null이다.
  final double? uploadFraction;

  bool get isActive => switch (stage) {
    EvaluationProgressStage.uploading ||
    EvaluationProgressStage.creatingJob ||
    EvaluationProgressStage.queued ||
    EvaluationProgressStage.downloadingAudio ||
    EvaluationProgressStage.analyzing ||
    EvaluationProgressStage.preparingFeedback ||
    EvaluationProgressStage.finalizing => true,
    _ => false,
  };
}
