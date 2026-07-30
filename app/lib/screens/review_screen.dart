// 파일 의도: 실제 평가 기록을 독립 탭에서 조회하고 문장 전체 재연습을 연결한다.

import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';

import '../api/evaluation_api.dart';
import '../app/app_theme.dart';
import '../models/auth_session.dart';
import '../models/practice_history.dart';
import '../models/practice_sentence.dart';
import '../services/app_auth_service.dart';
import '../widgets/result_tile.dart';
import '../widgets/shared_widgets.dart';

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
            const SectionHeader(title: 'Recent practice'),
            const SizedBox(height: 10),
            AppCard(
              padding: EdgeInsets.zero,
              child: Column(
                children: [
                  for (var index = 0; index < items.length; index++)
                    _ReviewHistoryCard(
                      item: items[index],
                      showDivider: index != items.length - 1,
                    ),
                ],
              ),
            ),
            const SizedBox(height: 13),
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
  const _ReviewHistoryCard({required this.item, required this.showDivider});

  final PracticeHistoryItem item;
  final bool showDivider;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.card,
      child: InkWell(
        onTap: () => _showHistoryDetail(context, item),
        child: Container(
          constraints: const BoxConstraints(minHeight: 67),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          decoration: BoxDecoration(
            border:
                showDivider
                    ? const Border(bottom: BorderSide(color: AppColors.border))
                    : null,
          ),
          child: Row(
            children: [
              Container(
                width: 44,
                height: 44,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: AppColors.softBlue,
                  borderRadius: BorderRadius.circular(AppSizes.radiusControl),
                ),
                child: Text(
                  '${item.overallScore}',
                  style: const TextStyle(
                    color: AppColors.primaryDark,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.originalText,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 3),
                    Text(
                      item.createdAt == null
                          ? item.standardPronunciation
                          : _dateLabel(item.createdAt!),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ],
                ),
              ),
              const Icon(
                Icons.chevron_right_rounded,
                color: AppColors.textPrimary,
                size: 20,
              ),
            ],
          ),
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
    builder:
        (context) => SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(AppSpacing.xl),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.originalText,
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: AppSpacing.sm),
                Text(item.summary),
                const SizedBox(height: AppSpacing.xxl),
                const SectionHeader(title: 'All syllable scores'),
                const SizedBox(height: AppSpacing.md),
                if (item.characters.isEmpty)
                  const StatePanel(
                    icon: Icons.grid_off_outlined,
                    title: 'Character-level scores are unavailable',
                  )
                else
                  Wrap(
                    spacing: AppSpacing.sm,
                    runSpacing: AppSpacing.sm,
                    children: [
                      for (final character in item.characters)
                        ResultTile(result: character),
                    ],
                  ),
              ],
            ),
          ),
        ),
  );
}

String _dateLabel(DateTime value) {
  final local = value.toLocal();
  final month = local.month.toString().padLeft(2, '0');
  final day = local.day.toString().padLeft(2, '0');
  final hour = local.hour.toString().padLeft(2, '0');
  final minute = local.minute.toString().padLeft(2, '0');
  return '${local.year}-$month-$day $hour:$minute';
}
