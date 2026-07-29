// 파일 의도: 오늘 가능한 실제 연습 행동과 추천 문장을 한 화면에 정리한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../models/evaluation_progress.dart';
import '../models/practice_quota.dart';
import '../models/practice_sentence.dart';
import '../widgets/progress_panel.dart';
import '../widgets/sentence_card.dart';
import '../widgets/shared_widgets.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({
    super.key,
    required this.sentences,
    required this.isLoading,
    required this.errorText,
    required this.quota,
    required this.isLoadingQuota,
    required this.quotaErrorText,
    required this.evaluationProgress,
    required this.onRetry,
    required this.onRetryQuota,
    required this.onSelect,
    required this.onOpenPractice,
    required this.onOpenReview,
    this.displayName,
  });

  final List<PracticeSentence> sentences;
  final bool isLoading;
  final String? errorText;
  final PracticeQuota? quota;
  final bool isLoadingQuota;
  final String? quotaErrorText;
  final EvaluationProgress evaluationProgress;
  final VoidCallback onRetry;
  final VoidCallback onRetryQuota;
  final ValueChanged<PracticeSentence> onSelect;
  final VoidCallback onOpenPractice;
  final VoidCallback onOpenReview;
  final String? displayName;

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: () async {
        onRetry();
        onRetryQuota();
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(
          AppSpacing.xl,
          AppSpacing.lg,
          AppSpacing.xl,
          AppSpacing.section,
        ),
        children: [
          const Text(
            'LingKo',
            style: TextStyle(
              color: AppColors.textPrimary,
              fontSize: 28,
              fontWeight: FontWeight.w900,
            ),
          ),
          const SizedBox(height: AppSpacing.xl),
          Text(
            displayName == null || displayName!.trim().isEmpty
                ? 'Ready to practice Korean?'
                : 'Welcome back, ${displayName!.trim()}',
            style: Theme.of(context).textTheme.headlineMedium,
          ),
          const SizedBox(height: AppSpacing.xs),
          Text(
            'Choose a sentence and focus on one clear attempt.',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: AppSpacing.xl),
          _QuotaSection(
            quota: quota,
            isLoading: isLoadingQuota,
            errorText: quotaErrorText,
            onRetry: onRetryQuota,
            onOpenReview: onOpenReview,
          ),
          if (evaluationProgress.isActive ||
              evaluationProgress.stage == EvaluationProgressStage.failed) ...[
            const SizedBox(height: AppSpacing.lg),
            _ActiveEvaluationCard(progress: evaluationProgress),
          ],
          const SizedBox(height: AppSpacing.section),
          const SectionHeader(title: 'Recommended for you'),
          const SizedBox(height: AppSpacing.md),
          if (isLoading)
            const StatePanel(
              icon: Icons.menu_book_outlined,
              title: 'Loading recommended sentences',
              message: 'Finding practice sentences for you.',
              isLoading: true,
            )
          else if (errorText != null)
            StatePanel(
              icon: Icons.wifi_off_outlined,
              title: 'Recommendations are unavailable',
              message: 'Check your connection and try again.',
              actionLabel: 'Retry',
              onAction: onRetry,
            )
          else if (sentences.isEmpty)
            StatePanel(
              icon: Icons.inbox_outlined,
              title: 'No recommended sentences yet',
              message: 'You can still enter your own Korean sentence.',
              actionLabel: 'Open Practice',
              onAction: onOpenPractice,
            )
          else ...[
            for (final sentence in sentences) ...[
              SentenceCard(sentence: sentence, onTap: () => onSelect(sentence)),
              const SizedBox(height: AppSpacing.md),
            ],
            PrimaryButton(
              label: 'Start Practice',
              icon: Icons.mic_none,
              onPressed: () => onSelect(sentences.first),
            ),
          ],
        ],
      ),
    );
  }
}

class _QuotaSection extends StatelessWidget {
  const _QuotaSection({
    required this.quota,
    required this.isLoading,
    required this.errorText,
    required this.onRetry,
    required this.onOpenReview,
  });

  final PracticeQuota? quota;
  final bool isLoading;
  final String? errorText;
  final VoidCallback onRetry;
  final VoidCallback onOpenReview;

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return const StatePanel(
        icon: Icons.mic_none,
        title: 'Loading practice quota',
        isLoading: true,
      );
    }
    if (errorText != null) {
      return StatePanel(
        icon: Icons.wifi_off_outlined,
        title: errorText!,
        message: 'Your practice availability could not be confirmed.',
        actionLabel: 'Retry',
        onAction: onRetry,
      );
    }
    final current = quota;
    if (current == null) {
      return const StatePanel(
        icon: Icons.info_outline,
        title: 'Practice quota unavailable',
      );
    }
    return Column(
      children: [
        ProgressPanel(
          remaining: current.remainingPractices,
          limit: current.freeLimit + current.rewardedAvailable,
          resetLabel:
              current.resetAt == null ? null : 'Resets at ${current.resetAt}',
        ),
        if (current.remainingPractices == 0) ...[
          const SizedBox(height: AppSpacing.md),
          SecondaryButton(
            label: 'Review previous practices',
            icon: Icons.history,
            onPressed: onOpenReview,
          ),
        ],
      ],
    );
  }
}

class _ActiveEvaluationCard extends StatelessWidget {
  const _ActiveEvaluationCard({required this.progress});

  final EvaluationProgress progress;

  @override
  Widget build(BuildContext context) {
    final failed = progress.stage == EvaluationProgressStage.failed;
    return AppCard(
      color:
          failed ? AppColors.error.withValues(alpha: 0.05) : AppColors.softBlue,
      child: Row(
        children: [
          Icon(
            failed ? Icons.error_outline : Icons.graphic_eq,
            color: failed ? AppColors.error : AppColors.primary,
          ),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  failed
                      ? 'Evaluation needs attention'
                      : 'Evaluation in progress',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  progress.message ??
                      'You can use other tabs while pronunciation is analyzed.',
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
