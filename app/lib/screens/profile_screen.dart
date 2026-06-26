import 'package:flutter/material.dart';

import '../api/evaluation_api.dart';
import '../app/app_theme.dart';
import '../models/practice_history.dart';
import '../models/practice_sentence.dart';
import '../widgets/settings_row.dart';
import '../widgets/shared_widgets.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({
    super.key,
    required this.evaluationApi,
    required this.onRetryPractice,
  });

  final EvaluationApi evaluationApi;
  final ValueChanged<PracticeSentence> onRetryPractice;

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
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
      final nextHistory = await widget.evaluationApi.fetchHistory();

      if (!mounted) {
        return;
      }

      setState(() {
        history = nextHistory;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        history = null;
        errorText = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 24),
      children: [
        const TopBar(title: 'Profile'),
        const SizedBox(height: 24),
        const SectionHeader(title: 'Practice history'),
        const SizedBox(height: 12),
        _HistorySummaryCard(history: history),
        const SizedBox(height: 12),
        if (isLoading)
          const _HistoryMessage(
            icon: Icons.history,
            label: 'Loading practice history',
          )
        else if (errorText != null)
          _HistoryMessage(
            icon: Icons.error_outline,
            label: errorText!,
            action: TextButton.icon(
              onPressed: loadHistory,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
            ),
          )
        else if (history == null || history!.items.isEmpty)
          const _HistoryMessage(
            icon: Icons.history_toggle_off,
            label: 'No practice history yet.',
          )
        else
          for (final item in history!.items) ...[
            _PracticeHistoryTile(
              item: item,
              onRetry: () => widget.onRetryPractice(item.toPracticeSentence()),
            ),
            const SizedBox(height: 10),
          ],
        const SizedBox(height: 24),
        Text('Settings', style: Theme.of(context).textTheme.headlineMedium),
        const SizedBox(height: 16),
        const SettingsRow(label: 'Display language', value: 'English'),
        const SettingsRow(label: 'Native language', value: 'English'),
        const SettingsRow(label: 'Target level', value: 'Beginner 2'),
      ],
    );
  }
}

class _HistorySummaryCard extends StatelessWidget {
  const _HistorySummaryCard({required this.history});

  final PracticeHistory? history;

  @override
  Widget build(BuildContext context) {
    final bestScore = history?.bestScore;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        children: [
          const Icon(Icons.emoji_events_outlined, color: AppColors.info),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              'Best score',
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ),
          Text(
            bestScore == null ? '-' : '$bestScore',
            style: Theme.of(context).textTheme.headlineMedium,
          ),
        ],
      ),
    );
  }
}

class _HistoryMessage extends StatelessWidget {
  const _HistoryMessage({required this.icon, required this.label, this.action});

  final IconData icon;
  final String label;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        children: [
          Icon(icon, color: AppColors.textSecondary),
          const SizedBox(height: 8),
          Text(label, style: Theme.of(context).textTheme.bodyMedium),
          if (action != null) ...[const SizedBox(height: 8), action!],
        ],
      ),
    );
  }
}

class _PracticeHistoryTile extends StatelessWidget {
  const _PracticeHistoryTile({required this.item, required this.onRetry});

  final PracticeHistoryItem item;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.background,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
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
              const SizedBox(width: 12),
              _ScoreBadge(score: item.overallScore),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            item.standardPronunciation,
            style: const TextStyle(
              color: AppColors.info,
              fontSize: 15,
              fontWeight: FontWeight.w700,
            ),
          ),
          if (item.recognizedText.isNotEmpty) ...[
            const SizedBox(height: 6),
            Text(
              'Recognized: ${item.recognizedText}',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
          ],
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(child: MetaPill(label: item.gradeLabel)),
              TextButton.icon(
                onPressed: onRetry,
                icon: const Icon(Icons.replay),
                label: const Text('Practice again'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _ScoreBadge extends StatelessWidget {
  const _ScoreBadge({required this.score});

  final int score;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 48,
      height: 48,
      decoration: BoxDecoration(
        color: AppColors.brandSoft,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Center(
        child: Text(
          '$score',
          style: const TextStyle(
            color: AppColors.textPrimary,
            fontSize: 18,
            fontWeight: FontWeight.w900,
          ),
        ),
      ),
    );
  }
}
