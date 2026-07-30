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
        padding: const EdgeInsets.fromLTRB(18, 15, 18, 22),
        children: [
          const Text(
            'LingKo',
            style: TextStyle(
              color: AppColors.textPrimary,
              fontSize: 25,
              fontWeight: FontWeight.w900,
              letterSpacing: -1.1,
            ),
          ),
          const SizedBox(height: 14),
          Text(
            displayName == null || displayName!.trim().isEmpty
                ? 'Ready to practice Korean? 👋'
                : 'Welcome back, ${displayName!.trim()} 👋',
            style: const TextStyle(
              color: AppColors.textPrimary,
              fontSize: 17,
              fontWeight: FontWeight.w900,
            ),
          ),
          const SizedBox(height: 15),
          _QuotaSection(
            quota: quota,
            isLoading: isLoadingQuota,
            errorText: quotaErrorText,
            onRetry: onRetryQuota,
            onOpenReview: onOpenReview,
          ),
          const SizedBox(height: 17),
          const SectionHeader(title: 'Recommended for you'),
          const SizedBox(height: 8),
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
            AppCard(
              key: const ValueKey('home-sentence-list'),
              padding: EdgeInsets.zero,
              child: Column(
                children: [
                  for (var index = 0; index < sentences.length; index++)
                    SentenceCard(
                      sentence: sentences[index],
                      onTap: () => onSelect(sentences[index]),
                      showDivider: index != sentences.length - 1,
                    ),
                ],
              ),
            ),
            const SizedBox(height: 13),
            PrimaryButton(
              label: 'Start Practice',
              icon: Icons.mic_none,
              onPressed: () => onSelect(sentences.first),
            ),
          ],
          if (evaluationProgress.isActive ||
              evaluationProgress.stage == EvaluationProgressStage.failed) ...[
            const SizedBox(height: 11),
            _ActiveEvaluationCard(
              progress: evaluationProgress,
              onTap: onOpenPractice,
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
  const _ActiveEvaluationCard({required this.progress, required this.onTap});

  final EvaluationProgress progress;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final failed = progress.stage == EvaluationProgressStage.failed;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppSizes.radius),
        child: AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          color: failed ? AppColors.errorSoft : AppColors.blue50,
          child: Row(
            children: [
              Container(
                width: 42,
                height: 42,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color:
                      failed
                          ? AppColors.error.withValues(alpha: 0.10)
                          : AppColors.softBlue,
                  borderRadius: BorderRadius.circular(AppSizes.radiusControl),
                ),
                child: Icon(
                  failed ? Icons.error_outline : Icons.graphic_eq,
                  color: failed ? AppColors.error : AppColors.primary,
                  size: 21,
                ),
              ),
              const SizedBox(width: 10),
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
                    const SizedBox(height: 2),
                    Text(
                      progress.message ??
                          'Your pronunciation is being analyzed.',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ],
                ),
              ),
              const Icon(
                Icons.chevron_right_rounded,
                color: AppColors.textPrimary,
                size: 20,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
