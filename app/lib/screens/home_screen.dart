import 'package:flutter/material.dart';

import '../models/practice_sentence.dart';
import '../widgets/progress_panel.dart';
import '../widgets/sentence_card.dart';
import '../widgets/shared_widgets.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({
    super.key,
    required this.sentences,
    required this.isLoading,
    required this.errorText,
    required this.onRetry,
    required this.onSelect,
  });

  final List<PracticeSentence> sentences;
  final bool isLoading;
  final String? errorText;
  final VoidCallback onRetry;
  final ValueChanged<PracticeSentence> onSelect;

  @override
  Widget build(BuildContext context) {
    // ListView는 세로 스크롤 화면입니다. 모바일 화면에서는 Column보다 안전합니다.
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 24),
      children: [
        Row(
          children: [
            const Expanded(
              child: Text(
                'LingKo',
                style: TextStyle(
                  color: Color(0xFF15324A),
                  fontSize: 32,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ),
            IconButton.filledTonal(
              onPressed: () {},
              icon: const Icon(Icons.notifications_none),
              tooltip: 'Notifications',
            ),
          ],
        ),
        const SizedBox(height: 18),
        Text('Today', style: Theme.of(context).textTheme.headlineLarge),
        const SizedBox(height: 10),
        const Text(
          '5 free practices left',
          style: TextStyle(
            color: Color(0xFF5C7286),
            fontSize: 15,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 24),
        const ProgressPanel(),
        const SizedBox(height: 26),
        SectionHeader(
          title: 'Recommended',
          trailing: TextButton(onPressed: () {}, child: const Text('All')),
        ),
        const SizedBox(height: 12),
        if (isLoading)
          const _HomeStatusCard(text: 'Loading recommended sentences')
        else if (errorText != null)
          _HomeStatusCard(
            text: errorText!,
            actionLabel: 'Retry',
            onAction: onRetry,
          )
        else if (sentences.isEmpty)
          const _HomeStatusCard(text: 'No recommended sentences yet.')
        else
          ...sentences.map(
            (sentence) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: SentenceCard(
                sentence: sentence,
                onTap: () => onSelect(sentence),
              ),
            ),
          ),
      ],
    );
  }
}

class _HomeStatusCard extends StatelessWidget {
  const _HomeStatusCard({required this.text, this.actionLabel, this.onAction});

  final String text;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFE1E8EF)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(text, style: Theme.of(context).textTheme.bodyLarge),
          if (actionLabel != null && onAction != null) ...[
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: onAction,
              icon: const Icon(Icons.refresh),
              label: Text(actionLabel!),
            ),
          ],
        ],
      ),
    );
  }
}
