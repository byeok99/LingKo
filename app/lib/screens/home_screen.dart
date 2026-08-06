// 파일 의도: 오늘 가능한 실제 연습 행동과 추천 문장을 한 화면에 정리한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import '../models/evaluation_progress.dart';
import '../models/practice_quota.dart';
import '../models/practice_sentence.dart';
import '../models/weak_word.dart';
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
    this.weakWords = const [],
    this.onSelectWeakWord,
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

  /// 반복해서 틀리는 어절이다. 비어 있으면 타일 영역을 그리지 않는다.
  final List<WeakWord> weakWords;
  final ValueChanged<WeakWord>? onSelectWeakWord;
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
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 22),
        children: [
          Row(
            children: [
              const Wordmark(),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: Align(
                  alignment: Alignment.centerRight,
                  child: ConstrainedBox(
                    // 캡슐은 [마이크 3/5 | 42:18 +] 순서라 자연 너비가 약 207px다.
                    // 좁게 잡으면 충전 버튼이 잘려 눌리지 않으므로 여유를 둔다.
                    constraints: const BoxConstraints(maxWidth: 215),
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
          const SizedBox(height: 18),
          // 인사는 이 화면에서 가장 큰 글자다. 무엇을 할 차례인지 먼저 말한다.
          Text(
            widget.displayName == null || widget.displayName!.trim().isEmpty
                ? 'What will you\nsay today?'
                : 'What will you\nsay today, ${widget.displayName!.trim()}?',
            style: Theme.of(context).textTheme.headlineLarge,
          ),
          if (widget.weakWords.isNotEmpty) ...[
            const SizedBox(height: 20),
            Container(
              padding: const EdgeInsets.symmetric(vertical: 16),
              decoration: BoxDecoration(
                border: Border(
                  top: BorderSide(color: context.palette.line),
                  bottom: BorderSide(color: context.palette.line),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const EyebrowLabel('Your weakest words · tap for detail'),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      for (var index = 0; index < widget.weakWords.length; index++) ...[
                        if (index > 0) const SizedBox(width: 9),
                        Expanded(
                          child: _WeakWordTile(
                            key: ValueKey(
                              'home-weak-word-${widget.weakWords[index].text}',
                            ),
                            word: widget.weakWords[index],
                            onTap:
                                () => widget.onSelectWeakWord?.call(
                                  widget.weakWords[index],
                                ),
                          ),
                        ),
                      ],
                    ],
                  ),
                ],
              ),
            ),
          ],
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
          SectionHeader(title: 'Practice by situation'),
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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // 칩 대신 밑줄 탭을 쓴다. 칩은 그 자체가 눌리는 덩어리로 보여
        // 문장 목록과 위계가 경쟁한다.
        SizedBox(
          height: AppSizes.minimumTouchTarget,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            itemCount: _SentenceCategory.values.length,
            separatorBuilder: (_, _) => const SizedBox(width: AppSpacing.lg),
            itemBuilder: (context, index) {
              final category = _SentenceCategory.values[index];
              return _CategoryTab(
                key: ValueKey('home-category-${category.name}'),
                label: category.label,
                selected: selectedCategory == category,
                onTap: () => selectCategory(category),
              );
            },
          ),
        ),
        const SizedBox(height: AppSpacing.md),
        if (categorySentences.isEmpty)
          StatePanel(
            icon: Icons.inbox_outlined,
            title: 'No ${selectedCategory.label} sentences yet',
            message: 'Choose another situation or practice your own sentence.',
          )
        else
          // 카드로 감싸지 않는다. 목록은 여백과 얇은 구분선으로 묶는 편이
          // 문장 자체를 읽는 데 방해가 적다.
          Column(
            key: const ValueKey('home-sentence-list'),
            children: [
              for (var index = 0; index < visibleSentences.length; index++)
                SentenceCard(
                  sentence: visibleSentences[index],
                  onTap: () => widget.onSelect(visibleSentences[index]),
                  showDivider: index != visibleSentences.length - 1,
                ),
            ],
          ),
        const SizedBox(height: AppSpacing.sm),
        // 부수적 이동은 채우지 않고 글자만 쓴다. 화면의 primary는 문장 선택 자체다.
        if (categorySentences.length > _sentencePreviewLimit)
          TextButton(
            key: const ValueKey('home-category-expand'),
            onPressed:
                () => setState(() => showAllSentences = !showAllSentences),
            child: Text(
              showAllSentences
                  ? 'Show fewer'
                  : 'Show ${categorySentences.length - _sentencePreviewLimit} more',
            ),
          ),
        TextButton(
          onPressed: widget.onOpenCustomPractice,
          child: const Text('Type my own sentence'),
        ),
      ],
    );
  }
}

/// 카테고리를 고르는 밑줄 탭이다.
///
/// 활성 표시를 배경 채움이 아니라 2px 밑줄로 하는 이유는, 채우면 그 자체가 눌러야 할
/// 대상처럼 보여 아래 문장 목록과 시선을 나눠 갖기 때문이다.
/// 반복해서 틀리는 어절 타일이다.
///
/// 음절이 아니라 어절인 이유는 신뢰할 수 있는 점수의 최소 단위가 어절이기 때문이다.
/// 없는 점수를 만들어 보여주지 않는다.
class _WeakWordTile extends StatelessWidget {
  const _WeakWordTile({super.key, required this.word, required this.onTap});

  final WeakWord word;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: '${word.text}, average ${word.averageScore}. Open detail.',
      excludeSemantics: true,
      child: Material(
        color: context.palette.errorSoft,
        borderRadius: BorderRadius.circular(AppSizes.radius),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppSizes.radius),
          child: Container(
            constraints: const BoxConstraints(minHeight: 52),
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  word.text,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: context.palette.error,
                    fontSize: 17,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  '${word.romanization} · ${word.averageScore}',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: context.palette.textSecondary,
                    fontSize: 10.5,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _CategoryTab extends StatelessWidget {
  const _CategoryTab({
    super.key,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      selected: selected,
      child: InkWell(
        onTap: onTap,
        child: Container(
          alignment: Alignment.center,
          padding: const EdgeInsets.symmetric(horizontal: 2),
          decoration: BoxDecoration(
            border: Border(
              bottom: BorderSide(
                color: selected ? context.palette.primary : Colors.transparent,
                width: 2,
              ),
            ),
          ),
          child: Text(
            label,
            style: TextStyle(
              fontSize: 14,
              fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
              color:
                  selected
                      ? context.palette.textPrimary
                      : context.palette.textMuted,
            ),
          ),
        ),
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
          color: failed ? context.palette.errorSoft : context.palette.blue50,
          child: Row(
            children: [
              Container(
                width: 42,
                height: 42,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color:
                      failed
                          ? context.palette.error.withValues(alpha: 0.10)
                          : context.palette.softBlue,
                  borderRadius: BorderRadius.circular(AppSizes.radiusControl),
                ),
                child: Icon(
                  failed ? Icons.error_outline : Icons.graphic_eq,
                  color: failed ? context.palette.error : context.palette.primary,
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
              Icon(
                Icons.chevron_right_rounded,
                color: context.palette.textPrimary,
                size: 20,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
