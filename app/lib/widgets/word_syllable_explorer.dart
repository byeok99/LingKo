// 파일 의도: 신뢰 가능한 단어 점수와 guide-only 음절 탐색을 하나의 일관된 UI로 제공한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../models/practice_result.dart';
import '../models/practice_sentence.dart';
import 'guide_sheet.dart';
import 'shared_widgets.dart';

/// 단어를 선택하면 그 단어에 속한 음절 가이드만 펼쳐 보여준다.
class WordSyllableExplorer extends StatefulWidget {
  const WordSyllableExplorer({super.key, required this.words});

  final List<PracticeWordResult> words;

  @override
  State<WordSyllableExplorer> createState() => _WordSyllableExplorerState();
}

class _WordSyllableExplorerState extends State<WordSyllableExplorer> {
  int selectedIndex = 0;

  @override
  void didUpdateWidget(covariant WordSyllableExplorer oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (selectedIndex >= widget.words.length || oldWidget.words != widget.words) {
      selectedIndex = 0;
    }
  }

  @override
  Widget build(BuildContext context) {
    if (widget.words.isEmpty) {
      return const StatePanel(
        icon: Icons.space_bar_outlined,
        title: 'Word feedback is unavailable',
        message: 'Evaluate this sentence again to receive word-level feedback.',
      );
    }

    final selectedWord = widget.words[selectedIndex];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Wrap(
          spacing: AppSpacing.sm,
          runSpacing: AppSpacing.sm,
          children: [
            for (var index = 0; index < widget.words.length; index++)
              _WordScoreButton(
                key: ValueKey('word-score-${widget.words[index].position}'),
                word: widget.words[index],
                selected: index == selectedIndex,
                onTap: () => setState(() => selectedIndex = index),
              ),
          ],
        ),
        const SizedBox(height: AppSpacing.md),
        AppCard(
          color: AppColors.blue50,
          padding: const EdgeInsets.all(AppSpacing.md),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                selectedWord.text,
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: AppSpacing.xs),
              Text(
                'Tap a syllable for its mouth and tongue guide.',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
              const SizedBox(height: AppSpacing.md),
              if (selectedWord.syllables.isEmpty)
                const Text('No syllable guide is available.')
              else
                Wrap(
                  spacing: AppSpacing.sm,
                  runSpacing: AppSpacing.sm,
                  children: [
                    for (
                      var index = 0;
                      index < selectedWord.syllables.length;
                      index++
                    )
                      _SyllableGuideButton(
                        key: ValueKey(
                          'syllable-guide-${selectedWord.position}-$index',
                        ),
                        syllable: selectedWord.syllables[index],
                      ),
                  ],
                ),
            ],
          ),
        ),
      ],
    );
  }
}

class _WordScoreButton extends StatelessWidget {
  const _WordScoreButton({
    super.key,
    required this.word,
    required this.selected,
    required this.onTap,
  });

  final PracticeWordResult word;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scoreAvailable =
        word.scoreStatus == 'AVAILABLE' && word.score != null;
    return Semantics(
      button: true,
      selected: selected,
      label:
          scoreAvailable
              ? '${word.text}, score ${word.score}. Show syllables.'
              : '${word.text}, score unavailable. Show syllables.',
      child: Material(
        color: selected ? AppColors.primary : AppColors.card,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSizes.radius),
          side: BorderSide(
            color: selected ? AppColors.primary : AppColors.border,
          ),
        ),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppSizes.radius),
          child: Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.md,
              vertical: AppSpacing.sm,
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  word.text,
                  style: TextStyle(
                    color: selected ? Colors.white : AppColors.textPrimary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(width: AppSpacing.sm),
                Text(
                  scoreAvailable ? '${word.score}' : '—',
                  style: TextStyle(
                    color: selected ? Colors.white : AppColors.primaryDark,
                    fontWeight: FontWeight.w900,
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

class _SyllableGuideButton extends StatelessWidget {
  const _SyllableGuideButton({super.key, required this.syllable});

  final CharacterResult syllable;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: '${syllable.character}. Open pronunciation guide.',
      child: Material(
        color: AppColors.card,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSizes.radius),
          side: const BorderSide(color: AppColors.border),
        ),
        child: InkWell(
          onTap: () => showModalBottomSheet<void>(
            context: context,
            isScrollControlled: true,
            backgroundColor: AppColors.card,
            shape: const RoundedRectangleBorder(
              borderRadius: BorderRadius.vertical(
                top: Radius.circular(AppSizes.radiusLarge),
              ),
            ),
            builder: (_) => GuideSheet(result: syllable),
          ),
          borderRadius: BorderRadius.circular(AppSizes.radius),
          child: Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.md,
              vertical: AppSpacing.sm,
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  syllable.character.isEmpty ? '—' : syllable.character,
                  style: const TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w900,
                  ),
                ),
                const SizedBox(width: AppSpacing.xs),
                const Icon(
                  Icons.chevron_right,
                  size: 17,
                  color: AppColors.primary,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
