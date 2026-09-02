// 파일 의도: 음성 업로드와 서버 처리의 진행 표시가 실제 정보 수준에 맞게 움직이는지 검증한다.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/app/app_theme.dart';
import 'package:lingko_app/models/evaluation_progress.dart';
import 'package:lingko_app/widgets/evaluation_progress_panel.dart';

void main() {
  testWidgets('upload ring animates to the measured byte progress', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: const Scaffold(
          body: EvaluationProgressPanel(
            progress: EvaluationProgress(
              stage: EvaluationProgressStage.uploading,
              uploadFraction: 0.5,
            ),
          ),
        ),
      ),
    );

    await tester.pump(const Duration(milliseconds: 400));

    final ring = tester.widget<CircularProgressIndicator>(
      find.byKey(const ValueKey('evaluation-progress-ring')),
    );
    expect(ring.value, closeTo(0.5, 0.01));
  });

  testWidgets('server processing uses a moving ring instead of fake percent', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: const Scaffold(
          body: EvaluationProgressPanel(
            progress: EvaluationProgress(
              stage: EvaluationProgressStage.analyzing,
              message: 'Checking accuracy, fluency, and completeness.',
            ),
          ),
        ),
      ),
    );

    final ring = tester.widget<CircularProgressIndicator>(
      find.byKey(const ValueKey('evaluation-progress-ring')),
    );
    expect(ring.value, isNull);
    expect(
      find.text('Checking accuracy, fluency, and completeness.'),
      findsOneWidget,
    );
  });
}
