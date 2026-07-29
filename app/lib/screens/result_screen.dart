// 파일 의도: 종합 점수와 모든 평가 음절을 숨김없이 보여주고 문장 전체 재연습을 연결한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../models/practice_result.dart';
import '../models/practice_sentence.dart';
import '../widgets/result_tile.dart';
import '../widgets/score_breakdown.dart';
import '../widgets/shared_widgets.dart';

class ResultScreen extends StatelessWidget {
  const ResultScreen({
    super.key,
    required this.sentence,
    required this.result,
    required this.onTryAgain,
    this.onOpenReview,
  });

  final PracticeSentence sentence;
  final PracticeResult? result;
  final VoidCallback onTryAgain;
  final VoidCallback? onOpenReview;

  @override
  Widget build(BuildContext context) {
    final currentResult = result;
    return ListView(
      padding: const EdgeInsets.fromLTRB(
        AppSpacing.xl,
        AppSpacing.lg,
        AppSpacing.xl,
        AppSpacing.section,
      ),
      children: [
        const TopBar(
          title: 'Result',
          subtitle: 'Review the whole sentence before your next attempt.',
        ),
        const SizedBox(height: AppSpacing.xxl),
        if (currentResult == null)
          StatePanel(
            icon: Icons.info_outline,
            title: 'Result data is unavailable',
            message: 'Return to Practice and evaluate the sentence again.',
            actionLabel: 'Try This Sentence Again',
            onAction: onTryAgain,
          )
        else ...[
          AppCard(
            child: Row(
              children: [
                ScoreRing(score: currentResult.overallScore),
                const SizedBox(width: AppSpacing.lg),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      StatusBadge(
                        label: currentResult.gradeLabel,
                        tone: _gradeTone(currentResult.overallScore),
                      ),
                      const SizedBox(height: AppSpacing.md),
                      Text(
                        currentResult.summary,
                        style: Theme.of(context).textTheme.bodyLarge,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          if (currentResult.recognizedText.isNotEmpty) ...[
            const SizedBox(height: AppSpacing.md),
            AppCard(
              color: AppColors.softBlue,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Recognized speech',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(currentResult.recognizedText),
                ],
              ),
            ),
          ],
          const SizedBox(height: AppSpacing.xxl),
          const SectionHeader(title: 'Score breakdown'),
          const SizedBox(height: AppSpacing.md),
          ScoreBreakdown(
            accuracy: currentResult.scoreBreakdown.accuracy,
            fluency: currentResult.scoreBreakdown.fluency,
            completeness: currentResult.scoreBreakdown.completeness,
          ),
          const SizedBox(height: AppSpacing.xxl),
          const SectionHeader(title: 'All syllable scores'),
          const SizedBox(height: AppSpacing.sm),
          Text(
            'Tap any syllable to open its available mouth and tongue guide.',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: AppSpacing.md),
          if (currentResult.characterScoreStatus == 'UNAVAILABLE' ||
              currentResult.characters.isEmpty)
            const StatePanel(
              icon: Icons.grid_off_outlined,
              title: 'Character-level scores are unavailable',
              message:
                  'No syllable score was returned for this evaluation. The overall result is still valid.',
            )
          else
            Wrap(
              key: const ValueKey('result-character-grid'),
              spacing: AppSpacing.sm,
              runSpacing: AppSpacing.sm,
              children: [
                for (final character in currentResult.characters)
                  ResultTile(result: character),
              ],
            ),
          const SizedBox(height: AppSpacing.section),
          PrimaryButton(
            key: const ValueKey('retry-whole-sentence'),
            label: 'Try This Sentence Again',
            icon: Icons.refresh,
            onPressed: onTryAgain,
          ),
          if (onOpenReview != null) ...[
            const SizedBox(height: AppSpacing.sm),
            SecondaryButton(
              label: 'Open Review',
              icon: Icons.history,
              onPressed: onOpenReview,
            ),
          ],
        ],
      ],
    );
  }
}

StatusTone _gradeTone(int score) {
  if (score >= 90) {
    return StatusTone.success;
  }
  if (score >= 75) {
    return StatusTone.info;
  }
  if (score >= 60) {
    return StatusTone.warning;
  }
  return StatusTone.error;
}
