// 파일 의도: 반복해서 틀리는 음절 하나를 파고들어 다음 연습 대상을 고르게 한다.
// 선택 이유: 점수만 보여주면 무엇을 해야 할지 알 수 없어, 과거 시도와 다음 후보를 함께 둔다.

import 'package:flutter/material.dart';

import '../api/practice_content_api.dart';
import '../app/app_palette.dart';
import '../app/app_theme.dart';
import '../models/practice_sentence.dart';
import '../models/weak_sound.dart';
import '../services/app_auth_service.dart';
import '../widgets/score_card.dart';
import '../widgets/sentence_card.dart';
import '../widgets/shared_widgets.dart';

/// 음절 상세 화면의 두 목록이다.
enum _SoundDetailTab { practiced, suggested }

/// 취약 음절 하나의 누적 성적과 연습 이력·다음 후보를 보여준다.
class SoundDetailScreen extends StatefulWidget {
  const SoundDetailScreen({
    super.key,
    required this.character,
    required this.practiceContentApi,
    required this.authService,
    required this.onSessionExpired,
    required this.onSelectSentence,
    required this.onClose,
  });

  final String character;
  final PracticeContentApi practiceContentApi;
  final AppAuthService authService;
  final VoidCallback onSessionExpired;
  final ValueChanged<PracticeSentence> onSelectSentence;
  final VoidCallback onClose;

  @override
  State<SoundDetailScreen> createState() => _SoundDetailScreenState();
}

class _SoundDetailScreenState extends State<SoundDetailScreen> {
  SoundDetail? detail;
  bool isLoading = true;
  String? errorText;
  _SoundDetailTab tab = _SoundDetailTab.practiced;

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    setState(() {
      isLoading = true;
      errorText = null;
    });
    try {
      final result = await widget.authService.runAuthenticated(
        (accessToken) => widget.practiceContentApi.fetchSoundDetail(
          accessToken: accessToken,
          character: widget.character,
        ),
      );
      if (mounted) {
        setState(() {
          detail = result;
          // 연습 이력이 없으면 빈 목록을 먼저 보여주는 대신 다음 후보로 연다.
          // 진입 직후 할 수 있는 일이 화면에 있어야 한다.
          tab =
              result.practiced.isEmpty
                  ? _SoundDetailTab.suggested
                  : _SoundDetailTab.practiced;
        });
      }
    } on AuthSessionExpiredException {
      widget.onSessionExpired();
    } catch (_) {
      if (mounted) {
        setState(() {
          errorText =
              'This sound could not be loaded. Check your connection and retry.';
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
    final current = detail;
    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 16, 18, 22),
      children: [
        TopBar(
          title: 'Sound',
          centered: true,
          leading: IconButton(
            key: const ValueKey('sound-detail-close'),
            tooltip: 'Back',
            onPressed: widget.onClose,
            icon: const Icon(Icons.chevron_left_rounded, size: 26),
          ),
        ),
        const SizedBox(height: 14),
        if (isLoading)
          const StatePanel(
            icon: Icons.trending_down_rounded,
            title: 'Loading this sound',
            isLoading: true,
          )
        else if (errorText != null)
          StatePanel(
            icon: Icons.wifi_off_outlined,
            title: errorText!,
            actionLabel: 'Retry',
            onAction: load,
          )
        else if (current != null) ...[
          _SoundHeader(detail: current),
          const SizedBox(height: 20),
          _TabBar(
            selected: tab,
            practicedCount: current.practiced.length,
            suggestedCount: current.suggested.length,
            onChanged: (next) => setState(() => tab = next),
          ),
          const SizedBox(height: 6),
          // 두 목록을 동시에 보여주지 않는다. 지나온 것과 앞으로 할 것은 성격이 달라
          // 나란히 두면 어느 쪽을 눌러야 할지 판단이 늦어진다.
          if (tab == _SoundDetailTab.practiced)
            _PracticedList(attempts: current.practiced)
          else
            _SuggestedList(
              sentences: current.suggested,
              onSelect: widget.onSelectSentence,
            ),
        ],
      ],
    );
  }
}

/// 음절과 누적 성적을 보여주는 머리말이다.
class _SoundHeader extends StatelessWidget {
  const _SoundHeader({required this.detail});

  final SoundDetail detail;

  @override
  Widget build(BuildContext context) {
    final hasScore = detail.attemptCount > 0;
    return AppCard(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: context.palette.errorSoft,
              borderRadius: BorderRadius.circular(AppSizes.radius),
            ),
            child: Text(
              detail.text,
              style: TextStyle(
                color: context.palette.error,
                fontSize: 26,
                fontWeight: FontWeight.w900,
                letterSpacing: -0.5,
              ),
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                RomanizationText(detail.romanization, fontSize: 13),
                const SizedBox(height: 6),
                Text(
                  // 시도가 없으면 평균을 0으로 보여주지 않는다. 측정하지 않은 값을
                  // 점수처럼 보여주면 사용자가 자기 실력으로 오해한다.
                  hasScore
                      ? 'Average ${detail.averageScore} across '
                          '${detail.attemptCount} '
                          '${detail.attemptCount == 1 ? 'try' : 'tries'}'
                      : 'Not practised yet',
                  style: TextStyle(
                    color: context.palette.textSecondary,
                    fontSize: 12.5,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _TabBar extends StatelessWidget {
  const _TabBar({
    required this.selected,
    required this.practicedCount,
    required this.suggestedCount,
    required this.onChanged,
  });

  final _SoundDetailTab selected;
  final int practicedCount;
  final int suggestedCount;
  final ValueChanged<_SoundDetailTab> onChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _Tab(
            key: const ValueKey('sound-detail-tab-practiced'),
            label: 'Practiced $practicedCount',
            selected: selected == _SoundDetailTab.practiced,
            onTap: () => onChanged(_SoundDetailTab.practiced),
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: _Tab(
            key: const ValueKey('sound-detail-tab-suggested'),
            label: 'Suggested $suggestedCount',
            selected: selected == _SoundDetailTab.suggested,
            onTap: () => onChanged(_SoundDetailTab.suggested),
          ),
        ),
      ],
    );
  }
}

class _Tab extends StatelessWidget {
  const _Tab({
    super.key,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      selected: selected,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppSizes.pillRadius),
        child: Container(
          constraints: const BoxConstraints(minHeight: 38),
          alignment: Alignment.center,
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
          decoration: BoxDecoration(
            color: selected ? context.palette.softBlue : context.palette.card,
            border: Border.all(
              color:
                  selected
                      ? context.palette.borderStrong
                      : context.palette.border,
            ),
            borderRadius: BorderRadius.circular(AppSizes.pillRadius),
          ),
          child: Text(
            label,
            style: TextStyle(
              fontSize: 14,
              fontWeight: selected ? FontWeight.w900 : FontWeight.w700,
              color:
                  selected
                      ? context.palette.primaryDark
                      : context.palette.textMuted,
            ),
          ),
        ),
      ),
    );
  }
}

class _PracticedList extends StatelessWidget {
  const _PracticedList({required this.attempts});

  final List<PracticedAttempt> attempts;

  @override
  Widget build(BuildContext context) {
    if (attempts.isEmpty) {
      return const StatePanel(
        icon: Icons.history_rounded,
        title: 'No attempts with this sound yet',
        message:
            'Practise one of the suggested sentences to see progress here.',
      );
    }

    return AppCard(
      padding: EdgeInsets.zero,
      child: Column(
        children: [
          for (var index = 0; index < attempts.length; index++)
            _AttemptRow(
              attempt: attempts[index],
              showDivider: index != attempts.length - 1,
            ),
        ],
      ),
    );
  }
}

class _AttemptRow extends StatelessWidget {
  const _AttemptRow({required this.attempt, required this.showDivider});

  final PracticedAttempt attempt;
  final bool showDivider;

  @override
  Widget build(BuildContext context) {
    final score = attempt.score;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
      decoration: BoxDecoration(
        border:
            showDivider
                ? Border(bottom: BorderSide(color: context.palette.lineSubtle))
                : null,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  attempt.originalText,
                  style: TextStyle(
                    color: context.palette.textPrimary,
                    fontSize: 17,
                    height: 1.35,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 5),
                RomanizationText(attempt.romanization, fontSize: 11),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                score == null ? '—' : '$score',
                style: TextStyle(
                  color:
                      score == null
                          ? context.palette.textMuted
                          : scoreColor(context, score),
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                  fontFeatures: const [FontFeature.tabularFigures()],
                ),
              ),
              const SizedBox(height: 4),
              Text(
                _formatDate(attempt.createdAt),
                style: TextStyle(
                  color: context.palette.textMuted,
                  fontSize: 11.5,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _SuggestedList extends StatelessWidget {
  const _SuggestedList({required this.sentences, required this.onSelect});

  final List<PracticeSentence> sentences;
  final ValueChanged<PracticeSentence> onSelect;

  @override
  Widget build(BuildContext context) {
    if (sentences.isEmpty) {
      return const StatePanel(
        icon: Icons.check_circle_outline,
        title: 'No new sentences with this sound',
        message: 'You have already practised every recommendation we have.',
      );
    }

    return AppCard(
      padding: EdgeInsets.zero,
      child: Column(
        children: [
          for (var index = 0; index < sentences.length; index++)
            SentenceCard(
              key: ValueKey(
                'sound-detail-suggested-${sentences[index].sentenceId}',
              ),
              sentence: sentences[index],
              onTap: () => onSelect(sentences[index]),
              showDivider: index != sentences.length - 1,
            ),
        ],
      ),
    );
  }
}

/// 목록에서는 연·월·일까지만 보여준다. 초 단위는 학습 이력에서 의미가 없다.
String _formatDate(DateTime? value) {
  if (value == null) {
    return '';
  }
  final month = value.month.toString().padLeft(2, '0');
  final day = value.day.toString().padLeft(2, '0');
  return '$month.$day';
}
