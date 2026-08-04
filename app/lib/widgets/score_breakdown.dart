// 파일 의도: 평가 API가 제공하는 점수 항목만 진행 막대로 표시한다.

import 'package:flutter/material.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import 'shared_widgets.dart';

class ScoreBreakdown extends StatelessWidget {
  const ScoreBreakdown({
    super.key,
    required this.accuracy,
    required this.fluency,
    required this.completeness,
  });

  final int accuracy;
  final int fluency;
  final int completeness;

  @override
  Widget build(BuildContext context) {
    final items = [
      ('Accuracy', accuracy),
      ('Fluency', fluency),
      ('Completeness', completeness),
    ];
    return AppCard(
      child: Column(
        children: [
          for (var index = 0; index < items.length; index++) ...[
            _ScoreProgressRow(label: items[index].$1, score: items[index].$2),
            if (index != items.length - 1) const SizedBox(height: 8),
          ],
        ],
      ),
    );
  }
}

class _ScoreProgressRow extends StatelessWidget {
  const _ScoreProgressRow({required this.label, required this.score});

  final String label;
  final int score;

  @override
  Widget build(BuildContext context) {
    final normalizedScore = score.clamp(0, 100);
    return Semantics(
      label: AppL10n.of(context).scoreOutOfHundred(label, normalizedScore),
      child: Row(
        children: [
          SizedBox(
            width: 88,
            child: Text(
              label,
              style: TextStyle(
                color: context.palette.textPrimary,
                fontSize: 12,
              ),
            ),
          ),
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(AppSizes.pillRadius),
              child: LinearProgressIndicator(
                value: normalizedScore / 100,
                minHeight: 8,
                backgroundColor: context.palette.border,
                color: context.palette.primary,
              ),
            ),
          ),
          const SizedBox(width: 9),
          Text(
            '$normalizedScore',
            style: TextStyle(
              color: context.palette.textPrimary,
              fontSize: 12,
              fontWeight: FontWeight.w900,
            ),
          ),
        ],
      ),
    );
  }
}
