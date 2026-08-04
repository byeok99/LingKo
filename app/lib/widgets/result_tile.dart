// 파일 의도: 모든 음절의 점수, 등급, 가이드 진입점을 같은 셀에서 제공한다.

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../models/practice_sentence.dart';
import 'guide_sheet.dart';
import 'shared_widgets.dart';

class ResultTile extends StatelessWidget {
  const ResultTile({super.key, required this.result, this.width = 64});

  final CharacterResult result;
  final double width;

  @override
  Widget build(BuildContext context) {
    final available = result.scoreStatus.isAvailable;
    final tone = _toneFor(result);
    final label = _labelFor(result);
    final color = switch (tone) {
      StatusTone.success => AppColors.success,
      StatusTone.warning => AppColors.warning,
      StatusTone.error => AppColors.error,
      StatusTone.info => AppColors.primary,
      StatusTone.neutral => AppColors.textSecondary,
    };
    return Semantics(
      button: true,
      label:
          available
              ? '${result.character}, score ${result.score}, $label. Open pronunciation guide.'
              : '${result.character}, score unavailable. Open pronunciation guide.',
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap:
              () => showGuideSheet(context, result),
          borderRadius: BorderRadius.circular(AppSizes.radius),
          child: Container(
            width: width,
            constraints: const BoxConstraints(minHeight: 61),
            padding: const EdgeInsets.symmetric(horizontal: 3, vertical: 8),
            decoration: BoxDecoration(
              color: _softColorFor(tone),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: color.withValues(alpha: 0.40)),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  result.character.isEmpty ? '—' : result.character,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w900,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  available ? '${result.score}' : '—',
                  style: TextStyle(
                    color: color,
                    fontSize: 11,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

Color _softColorFor(StatusTone tone) {
  return switch (tone) {
    StatusTone.success => AppColors.successSoft,
    StatusTone.warning || StatusTone.info => AppColors.warningSoft,
    StatusTone.error => AppColors.errorSoft,
    StatusTone.neutral => AppColors.surface,
  };
}

StatusTone _toneFor(CharacterResult result) {
  if (!result.scoreStatus.isAvailable) {
    return StatusTone.neutral;
  }
  if (result.score >= 90) {
    return StatusTone.success;
  }
  if (result.score >= 75) {
    return StatusTone.info;
  }
  if (result.score >= 60) {
    return StatusTone.warning;
  }
  return StatusTone.error;
}

String _labelFor(CharacterResult result) {
  if (!result.scoreStatus.isAvailable) {
    return 'No score';
  }
  if (result.score >= 90) {
    return 'Excellent';
  }
  if (result.score >= 75) {
    return 'Good';
  }
  if (result.score >= 60) {
    return 'Keep practicing';
  }
  return 'Needs improvement';
}
