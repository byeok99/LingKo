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
  });

  final PracticeSentence sentence;
  final PracticeResult? result;
  final VoidCallback onTryAgain;

  @override
  Widget build(BuildContext context) {
    final score = result?.overallScore ?? sentence.score;
    final gradeLabel = result?.gradeLabel ?? 'Good';
    final summary =
        result?.summary ??
        'Tense consonants and sibilant tongue position need attention.';
    final breakdown = result?.scoreBreakdown;
    final weak =
        result?.weakCharacters ??
        sentence.characters.where((item) => item.score < 80).toList();

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 24),
      children: [
        const TopBar(title: 'Result'),
        const SizedBox(height: 22),
        Row(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Text(
              '$score',
              style: const TextStyle(
                color: AppColors.textPrimary,
                fontSize: 64,
                fontWeight: FontWeight.w900,
                height: 0.95,
              ),
            ),
            const SizedBox(width: 8),
            Padding(
              padding: EdgeInsets.only(bottom: 8),
              child: Text(
                gradeLabel,
                style: const TextStyle(
                  color: AppColors.success,
                  fontSize: 18,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Text(
          summary,
          style: TextStyle(
            color: AppColors.textSecondary,
            fontSize: 16,
            height: 1.35,
          ),
        ),
        const SizedBox(height: 22),
        ScoreBreakdown(
          accuracy: breakdown?.accuracy ?? 84,
          fluency: breakdown?.fluency ?? 80,
          completeness: breakdown?.completeness ?? 91,
        ),
        const SizedBox(height: 28),
        const SectionHeader(title: 'Weak sounds'),
        const SizedBox(height: 12),
        ...weak.map(
          (item) => Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: ResultTile(result: item),
          ),
        ),
        const SizedBox(height: 18),
        FilledButton.icon(
          onPressed: onTryAgain,
          icon: const Icon(Icons.refresh),
          label: const Text('Try again'),
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.brand,
            foregroundColor: AppColors.textPrimary,
            minimumSize: const Size.fromHeight(54),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(8),
            ),
          ),
        ),
      ],
    );
  }
}
