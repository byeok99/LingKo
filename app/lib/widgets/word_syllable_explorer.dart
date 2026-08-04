// 파일 의도: 신뢰 가능한 단어 점수와 guide-only 음절 탐색을 하나의 일관된 UI로 제공한다.

import 'package:flutter/material.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
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
    // 새 평가 결과가 들어오면 이전 선택 위치가 다른 문장의 단어를 가리키게 되므로
    // 목록이 교체되거나 길이가 줄면 첫 단어로 되돌려 범위 밖 접근을 원천 차단한다.
    if (selectedIndex >= widget.words.length ||
        oldWidget.words != widget.words) {
      selectedIndex = 0;
    }
  }

  @override
  Widget build(BuildContext context) {
    if (widget.words.isEmpty) {
      return StatePanel(
        icon: Icons.space_bar_outlined,
        title: AppL10n.of(context).wordFeedbackIsUnavailable,
        message: AppL10n.of(context).evaluateThisSentenceAgainToReceive,
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
          color: context.palette.blue50,
          padding: const EdgeInsets.all(AppSpacing.md),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                selectedWord.text,
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: AppSpacing.md),
              if (selectedWord.syllables.isEmpty)
                Text(AppL10n.of(context).noSyllableGuideIsAvailable)
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
        word.scoreStatus.isAvailable && word.score != null;
    return Semantics(
      button: true,
      selected: selected,
      label:
          scoreAvailable
              ? '${word.text}, score ${word.score}. Show syllables.'
              : '${word.text}, score unavailable. Show syllables.',
      child: Material(
        color: selected ? context.palette.primary : context.palette.card,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSizes.radius),
          side: BorderSide(
            color: selected ? context.palette.primary : context.palette.border,
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
                    color: selected ? Colors.white : context.palette.textPrimary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(width: AppSpacing.sm),
                Text(
                  scoreAvailable ? '${word.score}' : '—',
                  style: TextStyle(
                    color: selected ? Colors.white : context.palette.primaryDark,
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
      label: AppL10n.of(context).openGuideForSyllable(syllable.character),
      child: Material(
        color: context.palette.card,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSizes.radius),
          side: BorderSide(color: context.palette.border),
        ),
        child: InkWell(
          onTap: () => showGuideSheet(context, syllable),
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
                  // 한글 음절은 획이 많아 라틴 문자와 같은 크기로는 자모를 구분하기 어렵다.
                  style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(width: AppSpacing.xs),
                Icon(
                  Icons.chevron_right,
                  size: 17,
                  color: context.palette.primary,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
