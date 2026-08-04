// 파일 의도: 실제 평가 기록을 독립 탭에서 조회하고 문장 전체 재연습을 연결한다.

import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';

import '../api/evaluation_api.dart';
import '../app/app_theme.dart';
import '../models/auth_session.dart';
import '../models/practice_history.dart';
import '../models/practice_result.dart';
import '../models/practice_sentence.dart';
import '../services/app_auth_service.dart';
import '../widgets/shared_widgets.dart';
import '../widgets/word_syllable_explorer.dart';

class ReviewScreen extends StatefulWidget {
  const ReviewScreen({
    super.key,
    required this.evaluationApi,
    required this.authService,
    required this.session,
    required this.onRetryPractice,
    required this.onSessionExpired,
  });

  final EvaluationApi evaluationApi;
  final AppAuthService authService;
  final AuthSession session;
  final ValueChanged<PracticeSentence> onRetryPractice;
  final VoidCallback onSessionExpired;

  @override
  State<ReviewScreen> createState() => _ReviewScreenState();
}

class _ReviewScreenState extends State<ReviewScreen> {
  PracticeHistory? history;
  bool isLoading = true;
  String? errorText;

  @override
  void initState() {
    super.initState();
    loadHistory();
  }

  Future<void> loadHistory() async {
    setState(() {
      isLoading = true;
      errorText = null;
    });
    try {
      final nextHistory = await widget.authService.runAuthenticated(
        (accessToken) =>
            widget.evaluationApi.fetchHistory(accessToken: accessToken),
      );
      if (mounted) {
        setState(() => history = nextHistory);
      }
    } on AuthSessionExpiredException {
      widget.onSessionExpired();
    } catch (_) {
      if (mounted) {
        setState(() {
          history = null;
          errorText =
              'Practice history could not be loaded. Check your connection and try again.';
        });
      }
    } finally {
      if (mounted) {
        setState(() => isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final items = history?.items ?? const <PracticeHistoryItem>[];
    return RefreshIndicator(
      onRefresh: loadHistory,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(18, 15, 18, 22),
        children: [
          const TopBar(title: 'Review'),
          const SizedBox(height: 10),
          if (isLoading)
            const StatePanel(
              icon: Icons.history,
              title: 'Loading practice history',
              isLoading: true,
            )
          else if (errorText != null)
            StatePanel(
              icon: Icons.wifi_off_outlined,
              title: 'History is unavailable',
              message: errorText,
              actionLabel: 'Retry',
              onAction: loadHistory,
            )
          else if (items.isEmpty)
            const StatePanel(
              icon: Icons.history_toggle_off,
              title: 'No practice history yet',
              message: 'Complete a pronunciation evaluation to see it here.',
            )
          else ...[
            _ReviewSummary(history: history!),
            const SizedBox(height: 24),
            SectionHeader(
              title: 'Recent practice',
              trailing: Text(
                '${items.length} ${items.length == 1 ? 'session' : 'sessions'}',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ),
            const SizedBox(height: 10),
            Column(
              children: [
                for (final item in items)
                  Padding(
                    padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                    child: _ReviewHistoryCard(item: item),
                  ),
              ],
            ),
            const SizedBox(height: AppSpacing.xs),
            SecondaryButton(
              label: 'Practice again',
              icon: Icons.replay,
              onPressed:
                  () =>
                      widget.onRetryPractice(items.first.toPracticeSentence()),
            ),
          ],
        ],
      ),
    );
  }
}

class _ReviewSummary extends StatelessWidget {
  const _ReviewSummary({required this.history});

  final PracticeHistory history;

  @override
  Widget build(BuildContext context) {
    final scores = recentTrendScores(history.items);
    final average =
        scores.isEmpty
            ? null
            : scores.reduce((left, right) => left + right) ~/ scores.length;
    final latestScore = scores.isEmpty ? null : scores.last;
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  'Recent scores · Last ${scores.length}',
                  style: const TextStyle(
                    color: AppColors.textSecondary,
                    fontSize: 12,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  const Text(
                    'Latest score',
                    style: TextStyle(
                      color: AppColors.textSecondary,
                      fontSize: 10,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  Text(
                    '${latestScore ?? '—'}',
                    style: const TextStyle(
                      color: AppColors.primaryDark,
                      fontSize: 14,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 12),
          Semantics(
            label:
                'Recent scores ${scores.join(', ')}, recent average ${average ?? 'unavailable'}, latest score ${latestScore ?? 'unavailable'}',
            child: SizedBox(
              height: 118,
              width: double.infinity,
              child: CustomPaint(painter: _TrendPainter(scores)),
            ),
          ),
        ],
      ),
    );
  }
}

/// 최신순 API 응답에서 최근 점수만 선택한 뒤 그래프의 시간축에 맞게 오래된 순으로 반환한다.
List<int> recentTrendScores(
  List<PracticeHistoryItem> newestFirstItems, {
  int maxPoints = 7,
}) {
  if (maxPoints <= 0 || newestFirstItems.isEmpty) {
    return const [];
  }

  return newestFirstItems
      .take(maxPoints)
      .map((item) => item.overallScore)
      .toList(growable: false)
      .reversed
      .toList(growable: false);
}

class _ReviewHistoryCard extends StatelessWidget {
  const _ReviewHistoryCard({required this.item});

  final PracticeHistoryItem item;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: EdgeInsets.zero,
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          key: ValueKey('review-history-card-${item.evaluationLogId}'),
          onTap: () => _showHistoryDetail(context, item),
          borderRadius: BorderRadius.circular(AppSizes.radius),
          child: Padding(
            padding: const EdgeInsets.all(AppSpacing.md),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 48,
                  constraints: const BoxConstraints(minHeight: 52),
                  padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: AppColors.softBlue,
                    borderRadius: BorderRadius.circular(AppSizes.radiusControl),
                  ),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        '${item.overallScore}',
                        style: const TextStyle(
                          color: AppColors.primaryDark,
                          fontSize: 16,
                          fontWeight: FontWeight.w900,
                          height: 1,
                        ),
                      ),
                      const SizedBox(height: 3),
                      const Text(
                        'SCORE',
                        style: TextStyle(
                          color: AppColors.textSecondary,
                          fontSize: 8,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 11),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.originalText,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: AppSpacing.xs),
                      Text(
                        item.standardPronunciation,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          color: AppColors.primaryMedium,
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: AppSpacing.sm),
                      Wrap(
                        spacing: AppSpacing.sm,
                        runSpacing: AppSpacing.xs,
                        crossAxisAlignment: WrapCrossAlignment.center,
                        children: [
                          _GradeChip(label: item.gradeLabel),
                          if (item.createdAt != null)
                            Text(
                              _dateLabel(item.createdAt!),
                              style: Theme.of(context).textTheme.bodyMedium,
                            ),
                        ],
                      ),
                    ],
                  ),
                ),
                const Padding(
                  padding: EdgeInsets.only(top: AppSpacing.md),
                  child: Icon(
                    Icons.chevron_right_rounded,
                    color: AppColors.textMuted,
                    size: 20,
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

class _GradeChip extends StatelessWidget {
  const _GradeChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSizes.pillRadius),
      ),
      child: Text(
        label,
        style: const TextStyle(
          color: AppColors.textSecondary,
          fontSize: 10,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _TrendPainter extends CustomPainter {
  const _TrendPainter(this.scores);

  final List<int> scores;

  @override
  void paint(Canvas canvas, Size size) {
    final gridPaint =
        Paint()
          ..color = AppColors.border
          ..strokeWidth = 1;
    final linePaint =
        Paint()
          ..color = AppColors.primary
          ..strokeWidth = 3
          ..style = PaintingStyle.stroke
          ..strokeCap = StrokeCap.round
          ..strokeJoin = StrokeJoin.round;
    final dotPaint =
        Paint()
          ..color = AppColors.primary
          ..style = PaintingStyle.fill;

    const chartTop = 14.0;
    final chartBottom = size.height - 18;
    canvas.drawLine(
      Offset(0, chartBottom),
      Offset(size.width, chartBottom),
      gridPaint,
    );
    canvas.drawLine(
      Offset(0, chartTop + (chartBottom - chartTop) / 2),
      Offset(size.width, chartTop + (chartBottom - chartTop) / 2),
      gridPaint,
    );
    if (scores.isEmpty) {
      return;
    }
    final points = <Offset>[
      for (var index = 0; index < scores.length; index++)
        Offset(
          scores.length == 1
              ? size.width / 2
              : size.width * index / (scores.length - 1),
          chartBottom -
              (chartBottom - chartTop) * scores[index].clamp(0, 100) / 100,
        ),
    ];
    final path = Path()..moveTo(points.first.dx, points.first.dy);
    for (final point in points.skip(1)) {
      path.lineTo(point.dx, point.dy);
    }
    canvas.drawPath(path, linePaint);
    for (var index = 0; index < points.length; index++) {
      final point = points[index];
      canvas.drawCircle(point, 4, dotPaint);
      final label = TextPainter(
        text: TextSpan(
          text: '${scores[index]}',
          style: const TextStyle(
            color: AppColors.textSecondary,
            fontSize: 10,
            fontWeight: FontWeight.w800,
          ),
        ),
        textDirection: TextDirection.ltr,
      )..layout();
      label.paint(
        canvas,
        Offset(
          (point.dx - label.width / 2).clamp(0, size.width - label.width),
          (point.dy - 17).clamp(0, chartBottom - label.height),
        ),
      );
    }
  }

  @override
  bool shouldRepaint(covariant _TrendPainter oldDelegate) {
    return !listEquals(oldDelegate.scores, scores);
  }
}

void _showHistoryDetail(BuildContext context, PracticeHistoryItem item) {
  showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: AppColors.card,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(
        top: Radius.circular(AppSizes.radiusLarge),
      ),
    ),
    builder: (context) => _HistoryDetailSheet(item: item),
  );
}

class _HistoryDetailSheet extends StatelessWidget {
  const _HistoryDetailSheet({required this.item});

  final PracticeHistoryItem item;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(
          AppSpacing.xl,
          AppSpacing.md,
          AppSpacing.xl,
          AppSpacing.xxl,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 42,
                height: 4,
                decoration: BoxDecoration(
                  color: AppColors.border,
                  borderRadius: BorderRadius.circular(AppSizes.pillRadius),
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.xl),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.originalText,
                        style: Theme.of(context).textTheme.headlineMedium,
                      ),
                      if (item.createdAt != null) ...[
                        const SizedBox(height: AppSpacing.xs),
                        Text(
                          _dateLabel(item.createdAt!),
                          style: Theme.of(context).textTheme.bodyMedium,
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(width: AppSpacing.lg),
                Container(
                  width: 58,
                  height: 58,
                  alignment: Alignment.center,
                  decoration: const BoxDecoration(
                    color: AppColors.softBlue,
                    shape: BoxShape.circle,
                  ),
                  child: Text(
                    '${item.overallScore}',
                    style: const TextStyle(
                      color: AppColors.primaryDark,
                      fontSize: 20,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.lg),
            AppCard(
              color: AppColors.softBlue,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Standard pronunciation',
                    style: TextStyle(
                      color: AppColors.textSecondary,
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Text(
                    item.standardPronunciation,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ],
              ),
            ),
            const SizedBox(height: AppSpacing.md),
            Text(item.summary, style: Theme.of(context).textTheme.bodyLarge),
            const SizedBox(height: AppSpacing.xxl),
            const SectionHeader(title: 'Score details'),
            const SizedBox(height: AppSpacing.md),
            _HistoryScoreBreakdown(breakdown: item.scoreBreakdown),
            const SizedBox(height: AppSpacing.xxl),
            const SectionHeader(title: 'Pronunciation by word'),
            const SizedBox(height: AppSpacing.md),
            WordSyllableExplorer(words: item.words),
          ],
        ),
      ),
    );
  }
}

class _HistoryScoreBreakdown extends StatelessWidget {
  const _HistoryScoreBreakdown({required this.breakdown});

  final PracticeScoreBreakdown breakdown;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _HistoryScoreMetric(
            label: 'Accuracy',
            score: breakdown.accuracy,
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        Expanded(
          child: _HistoryScoreMetric(
            label: 'Fluency',
            score: breakdown.fluency,
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        Expanded(
          child: _HistoryScoreMetric(
            label: 'Completeness',
            score: breakdown.completeness,
          ),
        ),
      ],
    );
  }
}

class _HistoryScoreMetric extends StatelessWidget {
  const _HistoryScoreMetric({required this.label, required this.score});

  final String label;
  final int score;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.xs,
        vertical: AppSpacing.md,
      ),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSizes.radiusControl),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        children: [
          Text(
            '$score',
            style: const TextStyle(
              color: AppColors.primaryDark,
              fontSize: 17,
              fontWeight: FontWeight.w900,
            ),
          ),
          const SizedBox(height: AppSpacing.xs),
          FittedBox(
            fit: BoxFit.scaleDown,
            child: Text(
              label,
              maxLines: 1,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ),
        ],
      ),
    );
  }
}

String _dateLabel(DateTime value) {
  final local = value.toLocal();
  final month = local.month.toString().padLeft(2, '0');
  final day = local.day.toString().padLeft(2, '0');
  final hour = local.hour.toString().padLeft(2, '0');
  final minute = local.minute.toString().padLeft(2, '0');
  return '${local.year}-$month-$day $hour:$minute';
}
