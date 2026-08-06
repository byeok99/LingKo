// 파일 의도: 오늘 가능한 실제 연습 행동과 추천 문장을 한 화면에 정리한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import '../models/evaluation_progress.dart';
import '../models/practice_quota.dart';
import '../models/practice_sentence.dart';
import '../models/weak_sound.dart';
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
    this.weakSounds = const [],
    this.onSelectWeakSound,
    this.savedSentenceIds = const {},
    this.onToggleSaved,
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
  /// 반복해서 틀리는 음절이다. 비어 있으면 이 블록 자체를 그리지 않는다.
  final List<WeakSound> weakSounds;
  final ValueChanged<WeakSound>? onSelectWeakSound;

  /// 저장한 문장의 식별자다. 행마다 서버에 묻지 않도록 shell이 한 번에 내려준다.
  final Set<int> savedSentenceIds;
  final ValueChanged<int>? onToggleSaved;
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
        padding: const EdgeInsets.fromLTRB(18, 16, 18, 22),
        children: [
          Row(
            children: [
              const Wordmark(),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: Align(
                  alignment: Alignment.centerRight,
                  child: ConstrainedBox(
                    // 수량과 timer를 세로로 두더라도 큰 글자에서는 폭이 늘어난다.
                    // 충전 버튼이 잘려 눌리지 않도록 header 안에서 충분한 상한을 둔다.
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
          // 짧은 인사로 문장 탐색보다 시선을 먼저 빼앗지 않게 한다.
          Text(
            widget.displayName == null || widget.displayName!.trim().isEmpty
                ? 'Good morning!'
                : 'Good morning, ${widget.displayName!.trim()}!',
            style: Theme.of(context).textTheme.headlineMedium,
          ),
          if (widget.weakSounds.isNotEmpty) ...[
            const SizedBox(height: 24),
            Padding(
              padding: const EdgeInsets.only(bottom: 2),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SectionHeader(title: 'Your weakest sounds'),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      for (
                        var index = 0;
                        index < widget.weakSounds.length;
                        index++
                      ) ...[
                        if (index > 0) const SizedBox(width: 9),
                        Expanded(
                          child: _WeakSoundTile(
                            key: ValueKey(
                              'home-weak-sound-${widget.weakSounds[index].text}',
                            ),
                            sound: widget.weakSounds[index],
                            onTap:
                                () => widget.onSelectWeakSound?.call(
                                  widget.weakSounds[index],
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
        SizedBox(
          height: 38,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            itemCount: _SentenceCategory.values.length,
            separatorBuilder: (_, _) => const SizedBox(width: AppSpacing.sm),
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
                    // 추천 문장만 저장할 수 있다. 직접 입력한 문장은 서버에 식별자가 없다.
                    isSaved:
                        visibleSentences[index].sentenceId == null
                            ? null
                            : widget.savedSentenceIds.contains(
                              visibleSentences[index].sentenceId,
                            ),
                    onToggleSaved: () {
                      final id = visibleSentences[index].sentenceId;
                      if (id != null) {
                        widget.onToggleSaved?.call(id);
                      }
                    },
                  ),
              ],
            ),
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
                  : 'Show ${categorySentences.length - _sentencePreviewLimit} more ${categorySentences.length - _sentencePreviewLimit == 1 ? 'sentence' : 'sentences'}',
            ),
          ),
        SecondaryButton(
          icon: Icons.edit_outlined,
          onPressed: widget.onOpenCustomPractice,
          label: 'Practice my own sentence',
        ),
      ],
    );
  }
}

/// 카테고리를 고르는 pill이다. 선택 상태는 파란 tint와 테두리를 함께 써 색각과 무관하게
/// 구분되도록 한다.
/// 반복해서 틀리는 어절 타일이다.
///
/// 음절이 아니라 어절인 이유는 신뢰할 수 있는 점수의 최소 단위가 어절이기 때문이다.
/// 없는 점수를 만들어 보여주지 않는다.
/// 취약 음절 하나를 여는 타일이다.
///
/// 점수를 "이 음절이 든 연습들의 평균"으로 읽히게 로마자와 한 줄에 묶는다. 음절 자체를
/// 측정한 값이 아니라서 점수만 크게 떼어 놓으면 측정값처럼 보인다.
class _WeakSoundTile extends StatelessWidget {
  const _WeakSoundTile({super.key, required this.sound, required this.onTap});

  final WeakSound sound;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label:
          '${sound.text}, average ${sound.averageScore} '
          'across ${sound.attemptCount} tries. Open detail.',
      excludeSemantics: true,
      child: Material(
        color: context.palette.blue50,
        borderRadius: BorderRadius.circular(AppSizes.radiusTile),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppSizes.radiusTile),
          child: Container(
            // 가로 여백을 두지 않는다. 세 타일이 같은 폭을 나눠 갖고 내용은 가운데
            // 정렬이라, 안쪽 여백을 주면 좁은 화면에서 로마자가 먼저 잘린다.
            padding: const EdgeInsets.symmetric(vertical: 12),
            decoration: BoxDecoration(
              border: Border.all(color: context.palette.blue200),
              borderRadius: BorderRadius.circular(AppSizes.radiusTile),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  sound.text,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: context.palette.error,
                    // 한 글자라 어절보다 크게 둘 수 있다. 타일에서 먼저 읽혀야 할 것이
                    // 소리 자체이므로 점수보다 무겁게 잡는다.
                    fontSize: 26,
                    height: 1.15,
                    fontWeight: FontWeight.w900,
                  ),
                ),
                const SizedBox(height: 5),
                Text(
                  '${sound.romanization} · ${sound.averageScore}',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: context.palette.textSecondary,
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    // 로마자는 소리를 끊어 읽는 표기라 자간을 벌려야 음절 경계가 보인다.
                    letterSpacing: 1.32,
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
        borderRadius: BorderRadius.circular(AppSizes.pillRadius),
        child: Container(
          alignment: Alignment.center,
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
          decoration: BoxDecoration(
            color: selected ? context.palette.softBlue : context.palette.card,
            border: Border.all(
              color:
                  selected
                      ? context.palette.borderStrong
                      : context.palette.border,
            ),
            borderRadius: BorderRadius.circular(AppSizes.pillRadius),
          ),
          child: Text(
            label,
            style: TextStyle(
              fontSize: 14,
              fontWeight: selected ? FontWeight.w900 : FontWeight.w700,
              color:
                  selected
                      ? context.palette.primaryDark
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
                  color:
                      failed ? context.palette.error : context.palette.primary,
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
