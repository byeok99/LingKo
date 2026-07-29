// 파일 의도: 비동기 평가 작업의 실제 진행 단계를 앱 shell과 여러 화면이 공유한다.

/// 서버 작업과 클라이언트 업로드 경계를 가짜 퍼센트 없이 표현한다.
enum EvaluationProgressStage {
  idle,
  uploading,
  creatingJob,
  analyzing,
  preparingFeedback,
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
  });

  final EvaluationProgressStage stage;
  final String? jobId;
  final String? message;
  final EvaluationProgressStage? failedAt;

  bool get isActive => switch (stage) {
    EvaluationProgressStage.uploading ||
    EvaluationProgressStage.creatingJob ||
    EvaluationProgressStage.analyzing ||
    EvaluationProgressStage.preparingFeedback => true,
    _ => false,
  };
}
