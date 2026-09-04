// 파일 의도: 서버 평가 phase가 사용자에게 보이는 진행 단계와 정확히 연결되는지 검증한다.

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/models/evaluation_job.dart';
import 'package:lingko_app/models/evaluation_progress.dart';

void main() {
  test('server guide phase maps to visible feedback preparation', () {
    const job = EvaluationJob(
      jobId: 'job-id',
      status: EvaluationJobStatus.processing,
      phase: EvaluationJobPhase.preparingGuides,
    );

    final progress = EvaluationProgress.fromJob(job);

    expect(progress.stage, EvaluationProgressStage.preparingFeedback);
    expect(progress.message, 'Building your personalized feedback.');
  });

  test('server finalizing phase remains visible before completion', () {
    const job = EvaluationJob(
      jobId: 'job-id',
      status: EvaluationJobStatus.processing,
      phase: EvaluationJobPhase.finalizing,
    );

    final progress = EvaluationProgress.fromJob(job);

    expect(progress.stage, EvaluationProgressStage.finalizing);
    expect(progress.isActive, isTrue);
  });
}
