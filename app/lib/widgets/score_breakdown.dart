// 파일 의도: score breakdown 표시 단위를 재사용 가능한 Widget으로 제공한다.
// 선택 이유: 화면의 상태 조율과 순수 표시를 분리하기 위해 작은 Widget 경계를 선택했다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';

/// Score Breakdown 표시를 재사용 가능한 Widget으로 제공한다.
/// 부모 화면의 업무 상태와 독립적으로 배치·표시 규칙을 검증하기 위해 분리했다.
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
