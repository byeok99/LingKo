// 파일 의도: 종합 점수와 모든 평가 음절을 숨김없이 보여주고 문장 전체 재연습을 연결한다.


import 'package:flutter/material.dart';

import '../app/app_theme.dart';
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
    this.onOpenReview,
  });

  final PracticeSentence sentence;
  final PracticeResult? result;
  final VoidCallback onTryAgain;

  /// 저장 상태다. null이면 저장할 수 없는 문장(직접 입력 등)이라 토글을 두지 않는다.
  final bool? isSaved;
  final VoidCallback? onToggleSaved;
  final VoidCallback? onOpenReview;

  @override
  Widget build(BuildContext context) {
    final currentResult = result;
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 22),
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
        const SizedBox(height: 10),
        if (currentResult == null)
          StatePanel(
            icon: Icons.info_outline,
            title: 'Result data is unavailable',
            message: 'Return to Practice and evaluate the sentence again.',
            actionLabel: 'Say it again',
            onAction: onTryAgain,
          )
        else ...[
          const SizedBox(height: 6),
          ScoreCard(
            overallScore: currentResult.overallScore,
            accuracy: currentResult.scoreBreakdown.accuracy,
            fluency: currentResult.scoreBreakdown.fluency,
            completeness: currentResult.scoreBreakdown.completeness,
            summary: currentResult.summary,
          ),
          const SizedBox(height: 15),
          // 원문과 표준 발음을 위아래로 붙여 어디가 달라지는지 눈으로 비교하게 한다.
          // 사용자가 실제로 낸 소리를 문자로 재현해 보여주지는 않는다.
          // 보정 없이 정확히 추출할 수 없어 틀린 정보를 사실처럼 보여주게 된다.
          const EyebrowLabel('How it should sound'),
          const SizedBox(height: 12),
          Text(
            sentence.text,
            style: TextStyle(
              color: context.palette.textPrimary,
              fontSize: 21,
              height: 1.5,
              fontWeight: FontWeight.w700,
              letterSpacing: -0.42,
            ),
          ),
          if (sentence.pronunciation.isNotEmpty) ...[
            const SizedBox(height: 5),
            Text(
              sentence.pronunciation,
              style: TextStyle(
                color: context.palette.textSecondary,
                fontSize: 21,
                height: 1.5,
                fontWeight: FontWeight.w500,
                letterSpacing: -0.42,
              ),
            ),
          ],
          if (sentence.romanization.isNotEmpty) ...[
            const SizedBox(height: 7),
            RomanizationText(
              sentence.romanization,
              key: const ValueKey('result-romanized-pronunciation'),
              fontSize: 11.5,
            ),
          ],
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.only(top: 12),
            decoration: BoxDecoration(
              border: Border(top: BorderSide(color: context.palette.line)),
            ),
            child: const EyebrowLabel('By word · tap to see its syllables'),
          ),
          const SizedBox(height: 6),
          WordSyllableExplorer(words: currentResult.words),
          const SizedBox(height: 20),
          PrimaryButton(
            key: const ValueKey('retry-whole-sentence'),
            label: 'Say it again',
            onPressed: onTryAgain,
          ),
          if (onOpenReview != null) ...[
            const SizedBox(height: AppSpacing.sm),
            TextButton(
              onPressed: onOpenReview,
              child: const Text('View review history'),
            ),
          ],
        ],
      ],
    );
  }
}

