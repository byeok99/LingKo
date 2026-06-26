import 'package:flutter/material.dart';

import '../app/app_theme.dart';

class ScoreBreakdown extends StatelessWidget {
  const ScoreBreakdown({
    super.key,
    this.accuracy = 84,
    this.fluency = 80,
    this.completeness = 91,
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

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        children:
            items
                .map(
                  (item) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 7),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(
                            item.$1,
                            style: Theme.of(context).textTheme.titleMedium,
                          ),
                        ),
                        Text(
                          '${item.$2}',
                          style: const TextStyle(
                            color: AppColors.textPrimary,
                            fontSize: 18,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ],
                    ),
                  ),
                )
                .toList(),
      ),
    );
  }
}
