// 파일 의도: 추천 문장과 기록 문장을 동일한 정보 계층으로 표시한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../models/practice_sentence.dart';
import 'shared_widgets.dart';

class SentenceCard extends StatelessWidget {
  const SentenceCard({
    super.key,
    required this.sentence,
    required this.onTap,
    this.actionLabel = 'Practice',
  });

  final PracticeSentence sentence;
  final VoidCallback onTap;
  final String actionLabel;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppSizes.radius),
        child: AppCard(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: AppSizes.minimumTouchTarget,
                height: AppSizes.minimumTouchTarget,
                decoration: const BoxDecoration(
                  color: AppColors.softBlue,
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.play_arrow, color: AppColors.primary),
              ),
              const SizedBox(width: AppSpacing.md),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      sentence.text,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    if (sentence.translation.isNotEmpty) ...[
                      const SizedBox(height: AppSpacing.xs),
                      Text(
                        sentence.translation,
                        style: Theme.of(context).textTheme.bodyMedium,
                      ),
                    ],
                    const SizedBox(height: AppSpacing.sm),
                    Text(
                      actionLabel,
                      style: const TextStyle(
                        color: AppColors.primary,
                        fontSize: 12,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: AppColors.textSecondary),
            ],
          ),
        ),
      ),
    );
  }
}
