// 파일 의도: 평가 결과의 총점과 세부 점수를 한 카드에서 한눈에 읽게 한다.

import 'package:flutter/material.dart';

import '../app/app_palette.dart';
import '../app/app_theme.dart';

/// 점수를 신뢰 구간이 아니라 두 단계로만 말한다.
///
/// 중간(노랑) 단계를 두지 않는 이유는 "애매함"이 사용자에게 다음 행동을 알려주지 않기
/// 때문이다. 다시 연습할지 넘어갈지만 판단하면 되므로 기준은 하나면 충분하다.
const int kPassingScore = 80;

Color scoreColor(BuildContext context, int score) {
  return score >= kPassingScore
      ? context.palette.success
      : context.palette.error;
}

/// 총점과 세부 점수 세 개를 담는 결과 카드다.
///
/// 총점을 60px로 크게 두고 세부 점수를 오른쪽에 묶는 이유는, 사용자가 먼저 알고 싶은 것이
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
      padding: const EdgeInsets.all(16),
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
              const SizedBox(width: 22),
              Expanded(
                child: Column(
                  children: [
                    _ScoreGauge(label: 'Accuracy', score: accuracy),
                    const SizedBox(height: 9),
                    _ScoreGauge(label: 'Fluency', score: fluency),
                    const SizedBox(height: 9),
                    _ScoreGauge(label: 'Full sentence', score: completeness),
                  ],
                ),
              ),
            ],
          ),
          if (summary.trim().isNotEmpty) ...[
            const SizedBox(height: 13),
            Container(
              padding: const EdgeInsets.only(top: 12),
              decoration: BoxDecoration(
                border: Border(
                  top: BorderSide(color: context.palette.lineSubtle),
                ),
              ),
              child: Text(
                summary,
                style: TextStyle(
                  color: color,
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
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
                    fontSize: 11,
                    height: 1.2,
                  ),
                ),
                const SizedBox(height: 4),
                ClipRRect(
                  borderRadius: BorderRadius.circular(AppSizes.pillRadius),
                  child: LinearProgressIndicator(
                    value: safeScore / 100,
                    minHeight: 5,
                    backgroundColor: context.palette.line,
                    color: scoreColor(context, safeScore),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 9),
          SizedBox(
            width: 26,
            child: Text(
              '$safeScore',
              textAlign: TextAlign.right,
              style: TextStyle(
                color: context.palette.textPrimary,
                fontSize: 12.5,
                fontWeight: FontWeight.w700,
                fontFeatures: const [FontFeature.tabularFigures()],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
