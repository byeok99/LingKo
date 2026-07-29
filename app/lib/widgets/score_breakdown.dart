// 파일 의도: 평가 API가 제공하는 점수 항목만 진행 막대로 표시한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
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
      ('Accuracy', accuracy, Icons.gps_fixed),
      ('Fluency', fluency, Icons.waves),
      ('Completeness', completeness, Icons.checklist),
    ];
    return AppCard(
      child: Column(
        children: [
          for (var index = 0; index < items.length; index++) ...[
            _ScoreProgressRow(
              label: items[index].$1,
              score: items[index].$2,
              icon: items[index].$3,
            ),
            if (index != items.length - 1)
              const SizedBox(height: AppSpacing.lg),
          ],
        ],
      ),
    );
  }
}

class _ScoreProgressRow extends StatelessWidget {
  const _ScoreProgressRow({
    required this.label,
    required this.score,
    required this.icon,
  });

  final String label;
  final int score;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    final normalizedScore = score.clamp(0, 100);
    return Semantics(
      label: '$label $normalizedScore out of 100',
      child: Row(
        children: [
          Icon(icon, color: AppColors.primary, size: 20),
          const SizedBox(width: AppSpacing.sm),
          SizedBox(
            width: 92,
            child: Text(label, style: Theme.of(context).textTheme.titleMedium),
          ),
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(AppSizes.pillRadius),
              child: LinearProgressIndicator(
                value: normalizedScore / 100,
                minHeight: 8,
                backgroundColor: AppColors.border,
                color: AppColors.primary,
              ),
            ),
          ),
          const SizedBox(width: AppSpacing.md),
          Text(
            '$normalizedScore',
            style: const TextStyle(fontWeight: FontWeight.w900),
          ),
        ],
      ),
    );
  }
}
