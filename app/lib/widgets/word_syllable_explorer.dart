// 파일 의도: 신뢰 가능한 단어 점수와 guide-only 음절 탐색을 하나의 일관된 UI로 제공한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import '../models/practice_result.dart';
import '../models/practice_sentence.dart';
import 'guide_sheet.dart';
import 'score_card.dart';
import 'shared_widgets.dart';

/// 어절 목록을 보여주고, 어절을 누르면 그 아래에 음절 칩을 펼친다.
///
/// 어절과 음절을 동시에 늘어놓지 않는 이유는 화면이 길어지는 것보다, 무엇이 상위이고
/// 무엇이 그 안에 있는지가 흐려지는 편이 더 나쁘기 때문이다. 한 번에 하나만 펼친다.
class WordSyllableExplorer extends StatefulWidget {
  const WordSyllableExplorer({super.key, required this.words});

  final List<PracticeWordResult> words;

  @override
  State<WordSyllableExplorer> createState() => _WordSyllableExplorerState();
}

class _WordSyllableExplorerState extends State<WordSyllableExplorer> {
  /// 펼친 어절의 위치다. null이면 모두 접힌 상태다.
  int? expandedIndex;

  @override
  void didUpdateWidget(covariant WordSyllableExplorer oldWidget) {
    super.didUpdateWidget(oldWidget);
    // 새 평가 결과가 들어오면 이전 선택이 다른 문장의 어절을 가리키게 되므로 접는다.
    if (oldWidget.words != widget.words) {
      expandedIndex = null;
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

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (var index = 0; index < widget.words.length; index++) ...[
          _WordRow(
            key: ValueKey('word-score-${widget.words[index].position}'),
            word: widget.words[index],
            showDivider: index != widget.words.length - 1,
            onTap:
                () => setState(
                  () => expandedIndex = expandedIndex == index ? null : index,
                ),
          ),
          if (expandedIndex == index)
            _SyllableChips(word: widget.words[index]),
        ],
      ],
    );
  }
}

/// 어절 한 줄이다. [어절 · 로마자 · 점수] 세 정보를 한 줄에 둔다.
class _WordRow extends StatelessWidget {
  const _WordRow({
    super.key,
    required this.word,
    required this.showDivider,
    required this.onTap,
  });

  final PracticeWordResult word;
  final bool showDivider;
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
      child: InkWell(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 9),
          decoration: BoxDecoration(
            border:
                showDivider
                    ? Border(
                      bottom: BorderSide(color: context.palette.lineSubtle),
                    )
                    : null,
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Flexible(
                child: Text(
                  word.text,
                  style: TextStyle(
                    color: textColor,
                    fontSize: 17,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: RomanizationText(word.romanization, fontSize: 11),
              ),
              const SizedBox(width: 12),
              SizedBox(
                width: 34,
                child: Text(
                  available ? '$score' : '—',
                  textAlign: TextAlign.right,
                  style: TextStyle(
                    color: available ? textColor : context.palette.textMuted,
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    fontFeatures: const [FontFeature.tabularFigures()],
                  ),
                ),
              ),
            ],
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
      padding: const EdgeInsets.only(top: 4, bottom: 10),
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
            highlighted ? context.palette.errorSoft : context.palette.neutralFill,
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
