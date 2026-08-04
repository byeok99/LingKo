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
    this.onContinueInBackground,
  });

  final EvaluationProgress progress;
  final VoidCallback? onRetry;
  final VoidCallback? onContinueInBackground;

  @override
  Widget build(BuildContext context) {
    final steps = const [
      (
        EvaluationProgressStage.uploading,
        'Uploading your recording',
        Icons.file_upload_outlined,
      ),
      (
        EvaluationProgressStage.creatingJob,
        'Sending it for evaluation',
        Icons.article_outlined,
      ),
      (
        EvaluationProgressStage.analyzing,
        'Analyzing your pronunciation',
        Icons.graphic_eq,
      ),
      (
        EvaluationProgressStage.preparingFeedback,
        'Preparing your feedback',
        Icons.view_module_outlined,
      ),
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
        Center(
          child: Container(
            width: 86,
            height: 86,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: AppColors.softBlue,
              borderRadius: BorderRadius.circular(28),
            ),
            child: const Icon(
              Icons.graphic_eq,
              size: 34,
              color: AppColors.primaryDark,
            ),
          ),
        ),
        const SizedBox(height: 14),
        Text(
          progress.stage == EvaluationProgressStage.failed
              ? 'We could not complete the evaluation'
              : "We're analyzing your pronunciation",
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.headlineMedium,
        ),
        const SizedBox(height: 8),
        // 실패 사유 같은 상황별 메시지가 있을 때만 보여준다. 정상 진행 중에는
        // 아래 안내 카드가 같은 내용을 이미 전달하므로 문장을 중복하지 않는다.
        if (progress.message != null) ...[
          Text(
            progress.message!,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 8),
        ],
        const SizedBox(height: 10),
        AppCard(
          padding: EdgeInsets.zero,
          child: Column(
            children: [
              for (var index = 0; index < steps.length; index++) ...[
                _EvaluationStepRow(
                  title: steps[index].$2,
                  leadingIcon: steps[index].$3,
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
                if (index != steps.length - 1) const Divider(),
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
        const SizedBox(height: 14),
        const AppCard(
          padding: EdgeInsets.all(13),
          color: AppColors.blue50,
          child: Row(
            children: [
              Icon(Icons.schedule_outlined, color: AppColors.primary, size: 20),
              SizedBox(width: 10),
              Expanded(
                child: Text('You can leave and come back. Nothing is lost.'),
              ),
            ],
          ),
        ),
        if (onContinueInBackground != null) ...[
          const SizedBox(height: 14),
          SecondaryButton(
            label: 'Continue in background',
            icon: Icons.arrow_back_rounded,
            onPressed: onContinueInBackground,
          ),
        ],
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
    required this.leadingIcon,
    required this.state,
  });

  final String title;
  final IconData leadingIcon;
  final _StepState state;

  @override
  Widget build(BuildContext context) {
    final color = switch (state) {
      _StepState.complete => AppColors.success,
      _StepState.active => AppColors.primary,
      _StepState.failed => AppColors.error,
      _StepState.pending => AppColors.disabled,
    };
    return Container(
      constraints: const BoxConstraints(minHeight: 56),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      child: Row(
        children: [
          Container(
            width: 42,
            height: 42,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: AppColors.softBlue,
              borderRadius: BorderRadius.circular(AppSizes.radiusControl),
            ),
            child: Icon(leadingIcon, color: AppColors.primaryDark, size: 20),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(title, style: Theme.of(context).textTheme.titleMedium),
          ),
          const SizedBox(width: 8),
          if (state == _StepState.active)
            const SizedBox.square(
              dimension: 24,
              child: CircularProgressIndicator(
                strokeWidth: 3,
                backgroundColor: AppColors.blue200,
                color: AppColors.primary,
              ),
            )
          else
            Container(
              width: 24,
              height: 24,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color:
                    state == _StepState.complete || state == _StepState.failed
                        ? color
                        : Colors.transparent,
                shape: BoxShape.circle,
              ),
              child: Icon(
                switch (state) {
                  _StepState.complete => Icons.check,
                  _StepState.failed => Icons.close,
                  _StepState.pending => Icons.more_horiz,
                  _StepState.active => Icons.circle,
                },
                color:
                    state == _StepState.complete || state == _StepState.failed
                        ? AppColors.card
                        : color,
                size: 15,
              ),
            ),
        ],
      ),
    );
  }
}
