// 파일 의도: 실제 평가 기록을 독립 탭에서 조회하고 문장 전체 재연습을 연결한다.

import 'package:flutter/material.dart';

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
        padding: const EdgeInsets.fromLTRB(
          AppSpacing.xl,
          AppSpacing.lg,
          AppSpacing.xl,
          AppSpacing.section,
        ),
        children: [
          const TopBar(
            title: 'Review',
            subtitle: 'Recent whole-sentence pronunciation results.',
          ),
          const SizedBox(height: AppSpacing.xxl),
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
            const SizedBox(height: AppSpacing.xxl),
            const SectionHeader(title: 'Recent practice'),
            const SizedBox(height: AppSpacing.md),
            for (final item in items) ...[
              _ReviewHistoryCard(
                item: item,
                onRetry:
                    () => widget.onRetryPractice(item.toPracticeSentence()),
              ),
              const SizedBox(height: AppSpacing.md),
            ],
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
    final average =
        history.items.isEmpty
            ? null
            : history.items
                    .map((item) => item.overallScore)
                    .reduce((left, right) => left + right) ~/
                history.items.length;
    return AppCard(
      color: AppColors.softBlue,
      child: Row(
        children: [
          Expanded(
            child: _SummaryValue(
              label: 'Recent average',
              value: average == null ? '—' : '$average',
            ),
          ),
          Container(width: 1, height: 48, color: AppColors.border),
          Expanded(
            child: _SummaryValue(
              label: 'Best score',
              value: history.bestScore == null ? '—' : '${history.bestScore}',
            ),
          ),
        ],
      ),
    );
  }
}

class _SummaryValue extends StatelessWidget {
  const _SummaryValue({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(value, style: Theme.of(context).textTheme.headlineMedium),
        const SizedBox(height: AppSpacing.xs),
        Text(label, style: Theme.of(context).textTheme.bodyMedium),
      ],
    );
  }
}

class _ReviewHistoryCard extends StatelessWidget {
  const _ReviewHistoryCard({required this.item, required this.onRetry});

  final PracticeHistoryItem item;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Text(
                  item.originalText,
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ),
              StatusBadge(
                label: '${item.overallScore}',
                tone:
                    item.overallScore >= 75
                        ? StatusTone.success
                        : StatusTone.warning,
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),
          Text(
            item.standardPronunciation,
            style: const TextStyle(
              color: AppColors.primaryDark,
              fontWeight: FontWeight.w700,
            ),
          ),
          if (item.createdAt != null) ...[
            const SizedBox(height: AppSpacing.xs),
            Text(
              _dateLabel(item.createdAt!),
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ],
          const SizedBox(height: AppSpacing.md),
          Row(
            children: [
              Expanded(
                child: TextButton(
                  onPressed: () => _showHistoryDetail(context, item),
                  child: const Text('View details'),
                ),
              ),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: onRetry,
                  icon: const Icon(Icons.replay),
                  label: const Text('Practice again'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
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
