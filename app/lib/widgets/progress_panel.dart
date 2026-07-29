// 파일 의도: 실제 일일 연습 할당량을 시각적으로 요약한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import 'shared_widgets.dart';

class ProgressPanel extends StatelessWidget {
  const ProgressPanel({
    super.key,
    required this.remaining,
    required this.limit,
    this.resetLabel,
  });

  final int remaining;
  final int limit;
  final String? resetLabel;

  @override
  Widget build(BuildContext context) {
    final safeLimit = limit <= 0 ? 1 : limit;
    final used = (safeLimit - remaining).clamp(0, safeLimit);
    return AppCard(
      color: AppColors.softBlue,
      child: Row(
        children: [
          SizedBox.square(
            dimension: 58,
            child: Stack(
              alignment: Alignment.center,
              children: [
                CircularProgressIndicator(
                  value: used / safeLimit,
                  strokeWidth: 6,
                  backgroundColor: AppColors.card,
                  color: AppColors.primary,
                ),
                Text(
                  '$remaining',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ],
            ),
          ),
          const SizedBox(width: AppSpacing.lg),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  remaining == 1
                      ? '1 practice left today'
                      : '$remaining practices left today',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  remaining == 0
                      ? (resetLabel ??
                          'Your daily practice limit resets later.')
                      : '$used of $safeLimit daily practices used',
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
