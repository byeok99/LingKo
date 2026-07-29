// 파일 의도: 업로드부터 결과 준비까지 실제 평가 작업 단계를 일관되게 표시한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../models/evaluation_progress.dart';
import 'shared_widgets.dart';

class EvaluationProgressPanel extends StatelessWidget {
  const EvaluationProgressPanel({
    super.key,
    required this.progress,
    this.onRetry,
  });

  final EvaluationProgress progress;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    final steps = const [
      (
        EvaluationProgressStage.uploading,
        'Audio upload',
        'Sending your recording securely.',
      ),
      (
        EvaluationProgressStage.creatingJob,
        'Evaluation job',
        'Creating one evaluation request.',
      ),
      (
        EvaluationProgressStage.analyzing,
        'Pronunciation analysis',
        'Analyzing your pronunciation.',
      ),
      (
        EvaluationProgressStage.preparingFeedback,
        'Feedback preparation',
        'Preparing your results.',
      ),
      (EvaluationProgressStage.completed, 'Complete', 'Your result is ready.'),
    ];
    final activeIndex = _stageIndex(
      progress.stage == EvaluationProgressStage.failed
          ? progress.failedAt ?? EvaluationProgressStage.analyzing
          : progress.stage,
    );
    return Column(
      key: const ValueKey('evaluation-progress'),
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Icon(Icons.graphic_eq, size: 48, color: AppColors.primary),
        const SizedBox(height: AppSpacing.md),
        Text(
          progress.stage == EvaluationProgressStage.failed
              ? 'We could not complete the evaluation'
              : 'Evaluating your pronunciation',
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.headlineMedium,
        ),
        const SizedBox(height: AppSpacing.sm),
        Text(
          progress.message ??
              'This can take a little while. You can visit another tab and come back.',
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodyMedium,
        ),
        const SizedBox(height: AppSpacing.xxl),
        AppCard(
          child: Column(
            children: [
              for (var index = 0; index < steps.length; index++) ...[
                _EvaluationStepRow(
                  title: steps[index].$2,
                  subtitle: steps[index].$3,
                  state:
                      progress.stage == EvaluationProgressStage.failed &&
                              index == activeIndex
                          ? _StepState.failed
                          : index < activeIndex
                          ? _StepState.complete
                          : index == activeIndex
                          ? _StepState.active
                          : _StepState.pending,
                ),
                if (index != steps.length - 1)
                  const Divider(height: AppSpacing.xxl),
              ],
            ],
          ),
        ),
        if (progress.stage == EvaluationProgressStage.failed &&
            onRetry != null) ...[
          const SizedBox(height: AppSpacing.lg),
          PrimaryButton(
            label: 'Retry with this recording',
            icon: Icons.refresh,
            onPressed: onRetry,
          ),
        ],
        const SizedBox(height: AppSpacing.lg),
        const AppCard(
          color: AppColors.softBlue,
          child: Row(
            children: [
              Icon(Icons.info_outline, color: AppColors.primary),
              SizedBox(width: AppSpacing.md),
              Expanded(
                child: Text(
                  'Submitting the same recording again while this job is active can create duplicate work.',
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

int _stageIndex(EvaluationProgressStage stage) {
  return switch (stage) {
    EvaluationProgressStage.idle || EvaluationProgressStage.uploading => 0,
    EvaluationProgressStage.creatingJob => 1,
    EvaluationProgressStage.analyzing => 2,
    EvaluationProgressStage.preparingFeedback => 3,
    EvaluationProgressStage.completed => 4,
    EvaluationProgressStage.failed => 2,
  };
}

enum _StepState { pending, active, complete, failed }

class _EvaluationStepRow extends StatelessWidget {
  const _EvaluationStepRow({
    required this.title,
    required this.subtitle,
    required this.state,
  });

  final String title;
  final String subtitle;
  final _StepState state;

  @override
  Widget build(BuildContext context) {
    final icon = switch (state) {
      _StepState.complete => Icons.check_circle,
      _StepState.active => Icons.radio_button_checked,
      _StepState.failed => Icons.error,
      _StepState.pending => Icons.radio_button_unchecked,
    };
    final color = switch (state) {
      _StepState.complete => AppColors.success,
      _StepState.active => AppColors.primary,
      _StepState.failed => AppColors.error,
      _StepState.pending => AppColors.disabled,
    };
    return Row(
      children: [
        Icon(icon, color: color),
        const SizedBox(width: AppSpacing.md),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: Theme.of(context).textTheme.titleMedium),
              Text(subtitle, style: Theme.of(context).textTheme.bodyMedium),
            ],
          ),
        ),
      ],
    );
  }
}
