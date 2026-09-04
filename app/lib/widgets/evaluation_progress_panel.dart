// 파일 의도: 업로드부터 결과 준비까지 실제 평가 작업 단계를 일관되게 표시한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import '../models/evaluation_progress.dart';
import 'shared_widgets.dart';

/// 서버가 제공하는 비동기 평가 단계를 진행 UI와 재시도·백그라운드 동작으로 연결한다.
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
        'Waiting for the evaluator',
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
      (
        EvaluationProgressStage.finalizing,
        'Finalizing your result',
        Icons.task_alt_outlined,
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
        // 채점 중에도 빠져나갈 길을 보이게 둔다. 스와이프만 두면 이탈 방법을
        // 모르는 사용자가 끝날 때까지 붙잡히게 된다.
        if (onContinueInBackground != null)
          Align(
            alignment: Alignment.centerLeft,
            child: IconButton(
              key: const ValueKey('scoring-back'),
              tooltip: 'Keep practising',
              onPressed: onContinueInBackground,
              icon: const Icon(Icons.chevron_left_rounded, size: 26),
            ),
          ),
        Center(
          child: SizedBox.square(
            dimension: 140,
            child: Stack(
              alignment: Alignment.center,
              children: [
                SizedBox.square(
                  dimension: 140,
                  child: _EvaluationProgressRing(progress: progress),
                ),
                Container(
                  width: 94,
                  height: 94,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: context.palette.softBlue,
                    shape: BoxShape.circle,
                  ),
                  child: AnimatedSwitcher(
                    duration: const Duration(milliseconds: 300),
                    child: Icon(
                      _stageIcon(progress.stage),
                      key: ValueKey(progress.stage),
                      size: 36,
                      color: context.palette.primaryDark,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 14),
        AnimatedSwitcher(
          duration: const Duration(milliseconds: 300),
          child: Text(
            progress.stage == EvaluationProgressStage.failed
                ? 'We could not complete the evaluation'
                : _stageTitle(progress.stage),
            key: ValueKey(progress.stage),
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.headlineMedium,
          ),
        ),
        const SizedBox(height: 8),
        // 실패 사유 같은 상황별 메시지가 있을 때만 보여준다. 정상 진행 중에는
        // 아래 안내 카드가 같은 내용을 이미 전달하므로 문장을 중복하지 않는다.
        if (progress.message != null) ...[
          AnimatedSwitcher(
            duration: const Duration(milliseconds: 300),
            child: Text(
              progress.message!,
              key: ValueKey(progress.message),
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
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
          PrimaryButton(label: 'Retry with this recording', onPressed: onRetry),
        ],
        const SizedBox(height: 14),
        Text(
          'You can leave and come back. Nothing is lost.',
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodyMedium,
        ),
      ],
    );
  }
}

int _stageIndex(EvaluationProgressStage stage) {
  return switch (stage) {
    EvaluationProgressStage.idle || EvaluationProgressStage.uploading => 0,
    EvaluationProgressStage.creatingJob || EvaluationProgressStage.queued => 1,
    EvaluationProgressStage.downloadingAudio ||
    EvaluationProgressStage.analyzing => 2,
    EvaluationProgressStage.preparingFeedback => 3,
    EvaluationProgressStage.finalizing => 4,
    EvaluationProgressStage.completed => 5,
    EvaluationProgressStage.failed => 2,
  };
}

IconData _stageIcon(EvaluationProgressStage stage) {
  return switch (stage) {
    EvaluationProgressStage.uploading => Icons.file_upload_outlined,
    EvaluationProgressStage.creatingJob ||
    EvaluationProgressStage.queued => Icons.schedule_rounded,
    EvaluationProgressStage.downloadingAudio => Icons.cloud_download_outlined,
    EvaluationProgressStage.analyzing => Icons.graphic_eq,
    EvaluationProgressStage.preparingFeedback => Icons.view_module_outlined,
    EvaluationProgressStage.finalizing ||
    EvaluationProgressStage.completed => Icons.task_alt_outlined,
    EvaluationProgressStage.failed => Icons.error_outline,
    EvaluationProgressStage.idle => Icons.mic_none,
  };
}

String _stageTitle(EvaluationProgressStage stage) {
  return switch (stage) {
    EvaluationProgressStage.uploading => 'Uploading your recording…',
    EvaluationProgressStage.creatingJob ||
    EvaluationProgressStage.queued => 'Waiting for your evaluation…',
    EvaluationProgressStage.downloadingAudio => 'Loading your recording…',
    EvaluationProgressStage.analyzing => 'Listening to your pronunciation…',
    EvaluationProgressStage.preparingFeedback => 'Preparing your feedback…',
    EvaluationProgressStage.finalizing => 'Almost ready…',
    EvaluationProgressStage.completed => 'Your result is ready',
    EvaluationProgressStage.failed => 'Evaluation failed',
    EvaluationProgressStage.idle => 'Ready to evaluate',
  };
}

/// 업로드 중에는 실제 byte 비율을 보간하고, 서버 작업은 완료율을 추측하지 않는 회전 ring으로 표시한다.
class _EvaluationProgressRing extends StatelessWidget {
  const _EvaluationProgressRing({required this.progress});

  final EvaluationProgress progress;

  @override
  Widget build(BuildContext context) {
    final uploadFraction =
        progress.stage == EvaluationProgressStage.uploading
            ? progress.uploadFraction
            : null;
    if (uploadFraction == null && progress.isActive) {
      return CircularProgressIndicator(
        key: const ValueKey('evaluation-progress-ring'),
        backgroundColor: context.palette.blue200,
        color: context.palette.primary,
        strokeWidth: 10,
        semanticsLabel: 'Evaluation in progress',
      );
    }

    if (uploadFraction == null) {
      // 완료·실패처럼 멈춘 상태에서는 영구 animation을 남기지 않아 접근성 도구와 화면 안정화를 방해하지 않는다.
      return CircularProgressIndicator(
        key: const ValueKey('evaluation-progress-ring'),
        value: progress.stage == EvaluationProgressStage.completed ? 1 : 0,
        backgroundColor: context.palette.blue200,
        color:
            progress.stage == EvaluationProgressStage.failed
                ? context.palette.error
                : context.palette.primary,
        strokeWidth: 10,
        semanticsLabel: 'Evaluation stopped',
      );
    }

    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: uploadFraction.clamp(0, 1).toDouble()),
      duration: const Duration(milliseconds: 350),
      curve: Curves.easeOutCubic,
      builder:
          (context, value, child) => CircularProgressIndicator(
            key: const ValueKey('evaluation-progress-ring'),
            value: value,
            backgroundColor: context.palette.blue200,
            color: context.palette.primary,
            strokeWidth: 10,
            semanticsLabel: 'Audio upload progress',
            semanticsValue: '${(value * 100).round()} percent',
          ),
    );
  }
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
      _StepState.complete => context.palette.success,
      _StepState.active => context.palette.primary,
      _StepState.failed => context.palette.error,
      _StepState.pending => context.palette.disabled,
    };
    return AnimatedContainer(
      duration: const Duration(milliseconds: 300),
      constraints: const BoxConstraints(minHeight: 56),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      child: Row(
        children: [
          Container(
            width: 42,
            height: 42,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: context.palette.softBlue,
              borderRadius: BorderRadius.circular(AppSizes.radiusControl),
            ),
            child: Icon(
              leadingIcon,
              color: context.palette.primaryDark,
              size: 20,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(title, style: Theme.of(context).textTheme.titleMedium),
          ),
          const SizedBox(width: 8),
          if (state == _StepState.active)
            SizedBox(
              width: 24,
              height: 24,
              child: Padding(
                padding: const EdgeInsets.all(4),
                child: CircularProgressIndicator(
                  color: context.palette.primary,
                  strokeWidth: 2,
                ),
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
                        ? context.palette.card
                        : color,
                size: 15,
              ),
            ),
        ],
      ),
    );
  }
}
