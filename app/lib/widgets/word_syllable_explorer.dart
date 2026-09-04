// 파일 의도: 신뢰 가능한 단어 점수와 guide-only 음절 탐색을 하나의 일관된 UI로 제공한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import '../models/practice_result.dart';
import '../models/practice_sentence.dart';
import 'guide_sheet.dart';
import 'score_card.dart';
import 'shared_widgets.dart';

/// 문장을 세로 어절 목록으로 훑고, 선택한 한 어절 행의 음절 guide만 펼쳐 보여준다.
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
    return AppCard(
      padding: EdgeInsets.zero,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(AppSizes.radius),
        child: Column(
          children: [
            for (var index = 0; index < widget.words.length; index++)
              _WordRow(
                key: ValueKey('word-score-${widget.words[index].position}'),
                word: widget.words[index],
                selected: safeIndex == index,
                showDivider: index != widget.words.length - 1,
                onTap: () => setState(() => selectedIndex = index),
              ),
          ],
        ),
      ),
    );
  }
}

/// 어절, 로마자, 점수를 한 줄로 훑고 선택한 행에서만 음절 guide를 펼친다.
class _WordRow extends StatelessWidget {
  const _WordRow({
    super.key,
    required this.word,
    required this.selected,
    required this.showDivider,
    required this.onTap,
  });

  final PracticeWordResult word;
  final bool selected;
  final bool showDivider;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final available = word.scoreStatus.isAvailable && word.score != null;
    final score = word.score;
    // 80점 미만 어절은 숫자와 어절을 같은 구간색으로 묶어 개선 대상을 바로 찾게 한다.
    final needsWork = available && score! < kPassingScore;
    final textColor =
        needsWork ? scoreColor(context, score) : context.palette.textPrimary;

    return Semantics(
      button: true,
      selected: selected,
      label:
          available
              ? '${word.text}, score $score. Show syllables.'
              : '${word.text}, score unavailable. Show syllables.',
      excludeSemantics: true,
      child: Material(
        color: selected ? context.palette.blue50 : context.palette.card,
        child: InkWell(
          onTap: onTap,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 11),
            decoration: BoxDecoration(
              border:
                  showDivider
                      ? Border(
                        bottom: BorderSide(color: context.palette.lineSubtle),
                      )
                      : null,
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        word.text,
                        style: TextStyle(
                          color: textColor,
                          fontSize: 16,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ),
                    if (word.romanization.trim().isNotEmpty) ...[
                      const SizedBox(width: 10),
                      Flexible(
                        child: RomanizationText(
                          word.romanization,
                          fontSize: 10,
                        ),
                      ),
                    ],
                    const SizedBox(width: 10),
                    SizedBox(
                      width: 40,
                      child: Text(
                        available ? '$score' : '—',
                        textAlign: TextAlign.right,
                        style: TextStyle(
                          color:
                              available
                                  ? scoreColor(context, score!)
                                  : context.palette.textMuted,
                          fontSize: 15,
                          fontWeight: FontWeight.w900,
                          fontFeatures: const [FontFeature.tabularFigures()],
                        ),
                      ),
                    ),
                  ],
                ),
                if (selected) ...[
                  const SizedBox(height: 9),
                  _SyllableChips(word: word),
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

    final score =
        word.scoreStatus.isAvailable && word.score != null ? word.score : null;

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
              score: score,
            ),
        ],
      ),
    );
  }
}

class _SyllableChip extends StatelessWidget {
  const _SyllableChip({super.key, required this.syllable, required this.score});

  final CharacterResult syllable;
  final int? score;

  @override
  Widget build(BuildContext context) {
    final highlighted = score != null && score! < kPassingScore;
    return Semantics(
      button: true,
      label: '${syllable.character}. Open pronunciation guide.',
      excludeSemantics: true,
      child: Material(
        color: context.palette.card,
        borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
        child: InkWell(
          onTap: () => showGuideSheet(context, syllable),
          borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
          child: Container(
            constraints: const BoxConstraints(minWidth: 44, minHeight: 44),
            padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 8),
            decoration: BoxDecoration(
              border: Border.all(
                color:
                    highlighted
                        ? scoreBorderColor(context, score!)
                        : context.palette.border,
              ),
              borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
            ),
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
                            ? scoreColor(context, score!)
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
