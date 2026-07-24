// 파일 의도: home screen 사용자 workflow와 화면 상태를 구성한다.
// 선택 이유: 화면은 상호작용과 표시 상태를 소유하고 네트워크·플랫폼 작업은 주입된 서비스에 위임한다.

import 'package:flutter/material.dart';

import '../models/practice_quota.dart';
import '../models/practice_sentence.dart';
import '../widgets/progress_panel.dart';
import '../widgets/sentence_card.dart';
import '../widgets/shared_widgets.dart';

/// Home Screen 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
class HomeScreen extends StatelessWidget {
  const HomeScreen({
    super.key,
    required this.sentences,
    required this.isLoading,
    required this.errorText,
    required this.quota,
    required this.isLoadingQuota,
    required this.quotaErrorText,
    required this.onRetry,
    required this.onRetryQuota,
    required this.onSelect,
  });

  final List<PracticeSentence> sentences;
  final bool isLoading;
  final String? errorText;
  final PracticeQuota? quota;
  final bool isLoadingQuota;
  final String? quotaErrorText;
  final VoidCallback onRetry;
  final VoidCallback onRetryQuota;
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
        _QuotaSummary(
          quota: quota,
          isLoading: isLoadingQuota,
          errorText: quotaErrorText,
          onRetry: onRetryQuota,
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

/// 할당량 Summary 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
class _QuotaSummary extends StatelessWidget {
  const _QuotaSummary({
    required this.quota,
    required this.isLoading,
    required this.errorText,
    required this.onRetry,
  });

  final PracticeQuota? quota;
  final bool isLoading;
  final String? errorText;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return const Text('Loading practice quota', style: _quotaTextStyle);
    }

    if (errorText != null) {
      return Row(
        children: [
          Expanded(
            child: Text(
              errorText!,
              style: _quotaTextStyle.copyWith(
                color: Theme.of(context).colorScheme.error,
              ),
            ),
          ),
          TextButton(onPressed: onRetry, child: const Text('Retry')),
        ],
      );
    }

    final remaining = quota?.remainingPractices;
    if (remaining == null) {
      return const Text('Sign in to track practices', style: _quotaTextStyle);
    }

    if (remaining == 0) {
      return const Text('No practices left today', style: _quotaTextStyle);
    }

    final label =
        remaining == 1
            ? '1 practice left today'
            : '$remaining practices left today';
    return Text(label, style: _quotaTextStyle);
  }
}

const _quotaTextStyle = TextStyle(
  color: Color(0xFF5C7286),
  fontSize: 15,
  fontWeight: FontWeight.w600,
);

/// Home Status Card 사용자 화면과 interaction 경계를 제공한다.
/// 표시 상태는 화면에 두고 외부 작업은 주입된 API·서비스에 위임한다.
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
