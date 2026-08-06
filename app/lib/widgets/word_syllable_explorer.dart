// 파일 의도: 신뢰 가능한 단어 점수와 guide-only 음절 탐색을 하나의 일관된 UI로 제공한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import '../models/practice_result.dart';
import '../models/practice_sentence.dart';
import 'guide_sheet.dart';
import 'score_card.dart';
import 'shared_widgets.dart';

/// 문장을 어절 pill로 먼저 훑고, 선택한 한 어절의 음절 guide만 아래에 보여준다.
///
/// API의 신뢰 가능한 점수 단위는 어절이므로 점수는 상단에만 둔다. 음절은 입·혀 guide를
/// 여는 탐색 단위로만 사용해 측정하지 않은 점수를 만들어 내지 않는다.
class WordSyllableExplorer extends StatefulWidget {
  const WordSyllableExplorer({super.key, required this.words});

  final List<PracticeWordResult> words;

  @override
  State<WordSyllableExplorer> createState() => _WordSyllableExplorerState();
}

class _WordSyllableExplorerState extends State<WordSyllableExplorer> {
  late int selectedIndex;

  @override
  void initState() {
    super.initState();
    selectedIndex = _initialIndex(widget.words);
  }

  @override
  void didUpdateWidget(covariant WordSyllableExplorer oldWidget) {
    super.didUpdateWidget(oldWidget);
    // 새 평가 결과가 들어오면 가장 도움이 필요한 어절로 선택을 다시 맞춘다.
    if (oldWidget.words != widget.words) {
      selectedIndex = _initialIndex(widget.words);
    }
  }

  int _initialIndex(List<PracticeWordResult> words) {
    if (words.isEmpty) {
      return 0;
    }
    var candidate = 0;
    int? lowest;
    for (var index = 0; index < words.length; index++) {
      final word = words[index];
      if (!word.scoreStatus.isAvailable || word.score == null) {
        continue;
      }
      if (lowest == null || word.score! < lowest) {
        lowest = word.score;
        candidate = index;
      }
    }
    return candidate;
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

    final safeIndex = selectedIndex.clamp(0, widget.words.length - 1);
    final selectedWord = widget.words[safeIndex];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (var index = 0; index < widget.words.length; index++)
              _WordChip(
                key: ValueKey('word-score-${widget.words[index].position}'),
                word: widget.words[index],
                selected: safeIndex == index,
                onTap: () => setState(() => selectedIndex = index),
              ),
          ],
        ),
        const SizedBox(height: AppSpacing.md),
        AppCard(
          color: context.palette.blue50,
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(
                    selectedWord.text,
                    style: TextStyle(
                      color: context.palette.textPrimary,
                      fontSize: 17,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                  const SizedBox(width: AppSpacing.sm),
                  Expanded(
                    child: RomanizationText(
                      selectedWord.romanization,
                      fontSize: 10.5,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              _SyllableChips(word: selectedWord),
            ],
          ),
        ),
      ],
    );
  }
}

/// 어절, 로마자, 점수를 한 덩어리로 읽게 하는 선택 pill이다.
class _WordChip extends StatelessWidget {
  const _WordChip({
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
    final available = word.scoreStatus.isAvailable && word.score != null;
    final score = word.score;
    // 점수가 낮은 어절은 숫자와 어절 텍스트를 같은 붉은색으로 칠해
    // 어디를 고쳐야 하는지 한 번에 보이게 한다.
    final failing = available && score! < kPassingScore;
    final textColor =
        failing ? context.palette.error : context.palette.textPrimary;

    return Semantics(
      button: true,
      label:
          available
              ? '${word.text}, score $score. Show syllables.'
              : '${word.text}, score unavailable. Show syllables.',
      excludeSemantics: true,
      child: Material(
        color:
            selected
                ? (failing
                    ? context.palette.errorSoft
                    : context.palette.softBlue)
                : context.palette.card,
        borderRadius: BorderRadius.circular(AppSizes.radiusControl),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppSizes.radiusControl),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 9),
            decoration: BoxDecoration(
              border: Border.all(
                color:
                    selected
                        ? (failing
                            ? context.palette.errorBorder
                            : context.palette.borderStrong)
                        : context.palette.border,
              ),
              borderRadius: BorderRadius.circular(AppSizes.radiusControl),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      word.text,
                      style: TextStyle(
                        color: textColor,
                        fontSize: 15,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    const SizedBox(width: 7),
                    Text(
                      available ? '$score' : '—',
                      style: TextStyle(
                        color:
                            available ? textColor : context.palette.textMuted,
                        fontSize: 12,
                        fontWeight: FontWeight.w900,
                        fontFeatures: const [FontFeature.tabularFigures()],
                      ),
                    ),
                  ],
                ),
                if (word.romanization.trim().isNotEmpty) ...[
                  const SizedBox(height: 3),
                  RomanizationText(word.romanization, fontSize: 9.5),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// 펼친 어절에 속한 음절 칩이다.
///
/// 음절에는 점수를 표시하지 않는다. 신뢰할 수 있는 점수의 단위가 어절이므로
/// 음절에 숫자를 붙이면 측정하지 않은 값을 measured처럼 보여주게 된다.
class _SyllableChips extends StatelessWidget {
  const _SyllableChips({required this.word});

  final PracticeWordResult word;

  @override
  Widget build(BuildContext context) {
    if (word.syllables.isEmpty) {
      return Padding(
        padding: const EdgeInsets.only(top: 6, bottom: 10),
        child: Text(
          'No syllable guide is available.',
          style: Theme.of(context).textTheme.bodyMedium,
        ),
      );
    }

    final failing =
        word.scoreStatus.isAvailable &&
        word.score != null &&
        word.score! < kPassingScore;

    return Padding(
      padding: EdgeInsets.zero,
      child: Wrap(
        spacing: 7,
        runSpacing: 7,
        children: [
          for (var index = 0; index < word.syllables.length; index++)
            _SyllableChip(
              key: ValueKey('syllable-guide-${word.position}-$index'),
              syllable: word.syllables[index],
              highlighted: failing,
            ),
        ],
      ),
    );
  }
}

class _SyllableChip extends StatelessWidget {
  const _SyllableChip({
    super.key,
    required this.syllable,
    required this.highlighted,
  });

  final CharacterResult syllable;
  final bool highlighted;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: '${syllable.character}. Open pronunciation guide.',
      excludeSemantics: true,
      child: Material(
        color:
            highlighted
                ? context.palette.errorSoft
                : context.palette.neutralFill,
        borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
        child: InkWell(
          onTap: () => showGuideSheet(context, syllable),
          borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
          child: Container(
            constraints: const BoxConstraints(minWidth: 44, minHeight: 44),
            padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 8),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  syllable.character.isEmpty ? '—' : syllable.character,
                  style: TextStyle(
                    // 한글은 획이 많아 라틴 문자와 같은 크기로는 자모를 구분하기 어렵다.
                    fontSize: 17,
                    fontWeight: FontWeight.w700,
                    color:
                        highlighted
                            ? context.palette.error
                            : context.palette.textPrimary,
                  ),
                ),
                const SizedBox(height: 3),
                RomanizationText(syllable.romanization, fontSize: 9.5),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
