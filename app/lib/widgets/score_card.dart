// 파일 의도: 평가 결과의 총점과 세부 점수를 한 카드에서 한눈에 읽게 한다.

import 'package:flutter/material.dart';

import '../app/app_palette.dart';
import '../app/app_theme.dart';

/// 점수 색상 구간이다. API의 0~100 점수를 모든 화면에서 같은 의미로 읽게 한다.
enum ScoreBand { low, medium, high }

const int kMediumScore = 60;
const int kPassingScore = 80;

ScoreBand scoreBand(int score) {
  if (score >= kPassingScore) {
    return ScoreBand.high;
  }
  if (score >= kMediumScore) {
    return ScoreBand.medium;
  }
  return ScoreBand.low;
}

Color scoreColor(BuildContext context, int score) {
  return switch (scoreBand(score)) {
    ScoreBand.high => context.palette.primaryDark,
    ScoreBand.medium => context.palette.scoreMedium,
    ScoreBand.low => context.palette.error,
  };
}

Color scoreSoftColor(BuildContext context, int score) {
  return switch (scoreBand(score)) {
    ScoreBand.high => context.palette.softBlue,
    ScoreBand.medium => context.palette.scoreMediumSoft,
    ScoreBand.low => context.palette.errorSoft,
  };
}

Color scoreBorderColor(BuildContext context, int score) {
  return switch (scoreBand(score)) {
    ScoreBand.high => context.palette.borderStrong,
    ScoreBand.medium => context.palette.scoreMediumBorder,
    ScoreBand.low => context.palette.errorBorder,
  };
}

Color scoreGaugeColor(BuildContext context, int score) {
  return switch (scoreBand(score)) {
    ScoreBand.high => context.palette.primary,
    ScoreBand.medium => context.palette.scoreMedium,
    ScoreBand.low => context.palette.error,
  };
}

/// 총점과 세부 점수 세 개를 담는 결과 카드다.
///
/// 총점을 62px로 크게 두고 세부 점수를 오른쪽에 묶는 이유는, 사용자가 먼저 알고 싶은 것이
/// "잘했나"이고 그다음이 "무엇이 부족했나"이기 때문이다. 같은 크기로 늘어놓으면 읽는
/// 순서가 생기지 않는다.
class ScoreCard extends StatelessWidget {
  const ScoreCard({
    super.key,
    required this.overallScore,
    required this.accuracy,
    required this.fluency,
    required this.completeness,
    required this.summary,
  });

  final int overallScore;
  final int accuracy;
  final int fluency;
  final int completeness;
  final String summary;

  @override
  Widget build(BuildContext context) {
    final color = scoreColor(context, overallScore);
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: context.palette.card,
        borderRadius: BorderRadius.circular(AppSizes.radius),
        border: Border.all(color: context.palette.border),
        boxShadow: [
          BoxShadow(
            color: context.palette.shadow,
            blurRadius: 20,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text(
                '$overallScore',
                style: TextStyle(
                  color: color,
                  fontSize: 62,
                  height: 1,
                  fontWeight: FontWeight.w900,
                  letterSpacing: -3,
                  // 점수가 바뀌어도 자리가 흔들리지 않도록 고정폭 숫자를 쓴다.
                  fontFeatures: const [FontFeature.tabularFigures()],
                ),
              ),
              const SizedBox(width: 20),
              Expanded(
                child: Column(
                  children: [
                    _ScoreGauge(label: 'Accuracy', score: accuracy),
                    const SizedBox(height: 11),
                    _ScoreGauge(label: 'Fluency', score: fluency),
                    const SizedBox(height: 11),
                    _ScoreGauge(label: 'Full sentence', score: completeness),
                  ],
                ),
              ),
            ],
          ),
          if (summary.trim().isNotEmpty) ...[
            const SizedBox(height: 15),
            Container(
              padding: const EdgeInsets.only(top: 13),
              decoration: BoxDecoration(
                border: Border(
                  top: BorderSide(color: context.palette.lineSubtle),
                ),
              ),
              child: Text(
                summary,
                style: TextStyle(
                  color: color,
                  fontSize: 14,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

/// 세부 점수 한 줄이다. 라벨 · 막대 · 숫자를 한 줄로 묶어 세 항목을 훑기 쉽게 한다.
class _ScoreGauge extends StatelessWidget {
  const _ScoreGauge({required this.label, required this.score});

  final String label;
  final int score;

  @override
  Widget build(BuildContext context) {
    final safeScore = score.clamp(0, 100);
    return Semantics(
      label: '$label $safeScore',
      excludeSemantics: true,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: TextStyle(
                    color: context.palette.textSecondary,
                    fontSize: 12.5,
                    height: 1.2,
                  ),
                ),
                const SizedBox(height: 6),
                ClipRRect(
                  borderRadius: BorderRadius.circular(AppSizes.pillRadius),
                  child: LinearProgressIndicator(
                    value: safeScore / 100,
                    minHeight: 7,
                    backgroundColor: context.palette.line,
                    color: scoreGaugeColor(context, safeScore),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          SizedBox(
            width: 34,
            child: Text(
              '$safeScore',
              textAlign: TextAlign.right,
              style: TextStyle(
                color: context.palette.textPrimary,
                fontSize: 15,
                fontWeight: FontWeight.w900,
                fontFeatures: const [FontFeature.tabularFigures()],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
