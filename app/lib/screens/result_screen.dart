// 파일 의도: 종합 점수와 모든 평가 음절을 숨김없이 보여주고 문장 전체 재연습을 연결한다.

import 'package:flutter/material.dart';

import '../app/app_palette.dart';
import '../models/practice_result.dart';
import '../models/practice_sentence.dart';
import '../widgets/score_card.dart';
import '../widgets/shared_widgets.dart';
import '../widgets/word_syllable_explorer.dart';

class ResultScreen extends StatelessWidget {
  const ResultScreen({
    super.key,
    required this.sentence,
    required this.result,
    required this.onTryAgain,
    this.isSaved,
    this.onToggleSaved,
  });

  final PracticeSentence sentence;
  final PracticeResult? result;
  final VoidCallback onTryAgain;

  /// 저장 상태다. null이면 저장할 수 없는 문장(직접 입력 등)이라 토글을 두지 않는다.
  final bool? isSaved;
  final VoidCallback? onToggleSaved;

  @override
  Widget build(BuildContext context) {
    final currentResult = result;
    return Padding(
      padding: const EdgeInsets.fromLTRB(18, 16, 18, 22),
      child: Column(
        children: [
          TopBar(
            title: 'Result',
            centered: true,
            // 방금 연습한 문장을 다시 하고 싶을 때가 저장할 마음이 가장 큰 순간이다.
            trailing:
                isSaved == null
                    ? null
                    : IconButton(
                      key: const ValueKey('result-save-sentence'),
                      tooltip: isSaved! ? 'Saved' : 'Save this sentence',
                      onPressed: onToggleSaved,
                      icon: Icon(
                        isSaved! ? Icons.bookmark : Icons.bookmark_border,
                        size: 20,
                        color:
                            isSaved!
                                ? context.palette.primary
                                : context.palette.textMuted,
                      ),
                    ),
          ),
          const SizedBox(height: 6),
          Expanded(
            child: ListView(
              padding: EdgeInsets.zero,
              children: [
                if (currentResult == null)
                  StatePanel(
                    icon: Icons.info_outline,
                    title: 'Result data is unavailable',
                    message:
                        'Return to Practice and evaluate the sentence again.',
                    actionLabel: 'Say it again',
                    onAction: onTryAgain,
                  )
                else ...[
                  ScoreCard(
                    overallScore: currentResult.overallScore,
                    accuracy: currentResult.scoreBreakdown.accuracy,
                    fluency: currentResult.scoreBreakdown.fluency,
                    completeness: currentResult.scoreBreakdown.completeness,
                    summary: currentResult.summary,
                  ),
                  const SizedBox(height: 6),
                  // 표준 발음은 별도 섹션 제목보다 카드 안의 짧은 label로 설명한다.
                  _StandardPronunciationCard(sentence: sentence),
                  const SizedBox(height: 15),
                  const SectionHeader(title: 'Pronunciation by word'),
                  const SizedBox(height: 6),
                  WordSyllableExplorer(words: currentResult.words),
                  const SizedBox(height: 12),
                ],
              ],
            ),
          ),
          if (currentResult != null) ...[
            const SizedBox(height: 12),
            // 결과 본문이 길어져도 다음 연습 진입점은 화면 바닥에 남긴다.
            PrimaryButton(
              key: const ValueKey('retry-whole-sentence'),
              label: 'Practice this sentence again',
              onPressed: onTryAgain,
            ),
          ],
        ],
      ),
    );
  }
}

/// 표준 발음과 로마자를 한 카드에서 바로 비교하게 한다.
class _StandardPronunciationCard extends StatelessWidget {
  const _StandardPronunciationCard({required this.sentence});

  final PracticeSentence sentence;

  @override
  Widget build(BuildContext context) {
    final pronunciation =
        sentence.pronunciation.trim().isEmpty
            ? sentence.text
            : sentence.pronunciation;
    return AppCard(
      color: context.palette.blue50,
      padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 13),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.graphic_eq_rounded,
                size: 14,
                color: context.palette.primaryDark,
              ),
              const SizedBox(width: 6),
              Text(
                'Standard pronunciation',
                style: TextStyle(
                  color: context.palette.primaryDark,
                  fontSize: 12,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            pronunciation,
            style: TextStyle(
              color: context.palette.textPrimary,
              fontSize: 22,
              height: 1.35,
              fontWeight: FontWeight.w900,
              letterSpacing: -0.44,
            ),
          ),
          if (sentence.romanization.isNotEmpty) ...[
            const SizedBox(height: 4),
            RomanizationText(
              sentence.romanization,
              key: const ValueKey('result-romanized-pronunciation'),
              fontSize: 11,
            ),
          ],
        ],
      ),
    );
  }
}
