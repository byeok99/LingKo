// 파일 의도: 오늘 가능한 실제 연습 행동과 추천 문장을 한 화면에 정리한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../models/evaluation_progress.dart';
import '../models/practice_quota.dart';
import '../models/practice_sentence.dart';
import '../widgets/progress_panel.dart';
import '../widgets/sentence_card.dart';
import '../widgets/shared_widgets.dart';

const int _sentencePreviewLimit = 3;

/// Home이 추천 문장을 한 축으로만 탐색하도록 지원하는 상황 카테고리다.
enum _SentenceCategory {
  daily('Daily', 'Daily Life'),
  food('Food', 'Food & Café'),
  travel('Travel', 'Travel & Directions'),
  study('Study', 'Learning Korean'),
  work('Work', 'Work'),
  health('Health', 'Health & Wellness');

  const _SentenceCategory(this.label, this.title);

  final String label;
  final String title;

  bool matches(PracticeSentence sentence) =>
      sentence.category.trim().toLowerCase() == label.toLowerCase();
}

/// 오늘의 연습 가능량과 상황별 추천 문장을 한 화면에서 탐색하게 한다.
class HomeScreen extends StatefulWidget {
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
    required this.onOpenCustomPractice,
    this.onRequestPracticeReward,
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
  final VoidCallback onOpenCustomPractice;
  final Future<void> Function()? onRequestPracticeReward;
  final String? displayName;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  late _SentenceCategory selectedCategory;
  bool showAllSentences = false;

  @override
  void initState() {
    super.initState();
    selectedCategory = _firstAvailableCategory(widget.sentences);
  }

  @override
  void didUpdateWidget(covariant HomeScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    final categoryStillAvailable = widget.sentences.any(
      selectedCategory.matches,
    );
    if (widget.sentences.isNotEmpty && !categoryStillAvailable) {
      selectedCategory = _firstAvailableCategory(widget.sentences);
      showAllSentences = false;
    }
  }

  _SentenceCategory _firstAvailableCategory(List<PracticeSentence> sentences) {
    for (final category in _SentenceCategory.values) {
      if (sentences.any(category.matches)) {
        return category;
      }
    }
    return _SentenceCategory.daily;
  }

  void selectCategory(_SentenceCategory category) {
    setState(() {
      selectedCategory = category;
      showAllSentences = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: () async {
        widget.onRetry();
        widget.onRetryQuota();
      },
      child: ListView(
        padding: const EdgeInsets.fromLTRB(18, 15, 18, 22),
        children: [
          Row(
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
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: Align(
                  alignment: Alignment.centerRight,
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 166),
                    child: MediaQuery.withClampedTextScaling(
                      maxScaleFactor: 1.2,
                      child: _QuotaSection(
                        quota: widget.quota,
                        isLoading: widget.isLoadingQuota,
                        errorText: widget.quotaErrorText,
                        onRetry: widget.onRetryQuota,
                        onRequestPracticeReward: widget.onRequestPracticeReward,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Text(
            widget.displayName == null || widget.displayName!.trim().isEmpty
                ? 'Ready to practice Korean? 👋'
                : 'Welcome back, ${widget.displayName!.trim()} 👋',
            style: const TextStyle(
              color: AppColors.textPrimary,
              fontSize: 17,
              fontWeight: FontWeight.w900,
            ),
          ),
          if (widget.evaluationProgress.isActive ||
              widget.evaluationProgress.stage ==
                  EvaluationProgressStage.failed) ...[
            const SizedBox(height: 11),
            _ActiveEvaluationCard(
              progress: widget.evaluationProgress,
              onTap: widget.onOpenPractice,
            ),
          ],
          const SizedBox(height: 17),
          const SectionHeader(title: 'Practice by situation'),
          const SizedBox(height: 10),
          if (widget.isLoading)
            const StatePanel(
              icon: Icons.menu_book_outlined,
              title: 'Loading recommended sentences',
              message: 'Finding practice sentences for you.',
              isLoading: true,
            )
          else if (widget.errorText != null)
            StatePanel(
              icon: Icons.wifi_off_outlined,
              title: 'Recommendations are unavailable',
              message: 'Check your connection and try again.',
              actionLabel: 'Retry',
              onAction: widget.onRetry,
            )
          else if (widget.sentences.isEmpty)
            StatePanel(
              icon: Icons.inbox_outlined,
              title: 'No recommended sentences yet',
              message: 'You can still enter your own Korean sentence.',
              actionLabel: 'Open Practice',
              onAction: widget.onOpenCustomPractice,
            )
          else
            _buildCategoryBrowser(context),
        ],
      ),
    );
  }

  Widget _buildCategoryBrowser(BuildContext context) {
    final categorySentences = widget.sentences
        .where(selectedCategory.matches)
        .toList(growable: false);
    final visibleSentences =
        showAllSentences
            ? categorySentences
            : categorySentences
                .take(_sentencePreviewLimit)
                .toList(growable: false);
    final countLabel =
        '${categorySentences.length} ${categorySentences.length == 1 ? 'sentence' : 'sentences'}';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SizedBox(
          height: AppSizes.minimumTouchTarget,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            itemCount: _SentenceCategory.values.length,
            separatorBuilder: (_, _) => const SizedBox(width: AppSpacing.sm),
            itemBuilder: (context, index) {
              final category = _SentenceCategory.values[index];
              return ChoiceChip(
                key: ValueKey('home-category-${category.name}'),
                label: Text(category.label),
                selected: selectedCategory == category,
                onSelected: (_) => selectCategory(category),
                showCheckmark: true,
              );
            },
          ),
        ),
        const SizedBox(height: AppSpacing.md),
        SectionHeader(
          title: selectedCategory.title,
          trailing: Text(
            countLabel,
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ),
        const SizedBox(height: AppSpacing.sm),
        if (categorySentences.isEmpty)
          StatePanel(
            icon: Icons.inbox_outlined,
            title: 'No ${selectedCategory.label} sentences yet',
            message: 'Choose another situation or practice your own sentence.',
          )
        else
          AppCard(
            key: const ValueKey('home-sentence-list'),
            padding: EdgeInsets.zero,
            child: Column(
              children: [
                for (var index = 0; index < visibleSentences.length; index++)
                  SentenceCard(
                    sentence: visibleSentences[index],
                    onTap: () => widget.onSelect(visibleSentences[index]),
                    showDivider: index != visibleSentences.length - 1,
                  ),
              ],
            ),
          ),
        if (categorySentences.length > _sentencePreviewLimit) ...[
          const SizedBox(height: AppSpacing.xs),
          TextButton(
            key: const ValueKey('home-category-expand'),
            onPressed:
                () => setState(() => showAllSentences = !showAllSentences),
            child: Text(
              showAllSentences ? 'Show fewer' : 'View all $countLabel',
            ),
          ),
        ],
        const SizedBox(height: AppSpacing.sm),
        SecondaryButton(
          label: 'Practice my own sentence',
          icon: Icons.edit_outlined,
          onPressed: widget.onOpenCustomPractice,
        ),
      ],
    );
  }
}

class _QuotaSection extends StatelessWidget {
  const _QuotaSection({
    required this.quota,
    required this.isLoading,
    required this.errorText,
    required this.onRetry,
    this.onRequestPracticeReward,
  });

  final PracticeQuota? quota;
  final bool isLoading;
  final String? errorText;
  final VoidCallback onRetry;
  final Future<void> Function()? onRequestPracticeReward;

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return Semantics(
        label: 'Loading practice energy',
        child: const SizedBox(
          height: 40,
          child: Center(
            child: SizedBox.square(
              dimension: 20,
              child: CircularProgressIndicator(strokeWidth: 2.5),
            ),
          ),
        ),
      );
    }
    if (errorText != null) {
      return Semantics(
        label: errorText,
        button: true,
        container: true,
        excludeSemantics: true,
        child: SizedBox(
          height: 40,
          child: Align(
            alignment: Alignment.centerRight,
            child: IconButton.filledTonal(
              onPressed: onRetry,
              tooltip: 'Retry practice energy',
              style: IconButton.styleFrom(
                minimumSize: const Size.square(32),
                maximumSize: const Size.square(32),
                padding: EdgeInsets.zero,
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
              icon: const Icon(Icons.refresh_rounded, size: 18),
            ),
          ),
        ),
      );
    }
    final current = quota;
    if (current == null) {
      return Semantics(
        label: 'Practice energy unavailable',
        child: const SizedBox(height: 40),
      );
    }
    return ProgressPanel(
      remaining: current.remainingPractices,
      limit: current.freeLimit,
      timeUntilNextRefill: current.timeUntilNextRefill,
      onRefillDue: onRetry,
      onRequestAdReward: onRequestPracticeReward,
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
