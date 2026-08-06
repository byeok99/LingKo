// 파일 의도: 저장한 문장을 모아 보고 다시 연습하거나 저장을 해제한다.
// 선택 이유: 저장은 여러 화면에서 켜지지만 해제할 자리가 없으면 목록이 계속 자란다.

import 'package:flutter/material.dart';

import '../api/practice_content_api.dart';
import '../models/practice_sentence.dart';
import '../services/app_auth_service.dart';
import '../widgets/sentence_card.dart';
import '../widgets/shared_widgets.dart';

/// 저장한 문장 목록 화면이다.
class SavedSentencesScreen extends StatefulWidget {
  const SavedSentencesScreen({
    super.key,
    required this.practiceContentApi,
    required this.authService,
    required this.onSessionExpired,
    required this.onSelect,
    required this.onClose,
  });

  final PracticeContentApi practiceContentApi;
  final AppAuthService authService;
  final VoidCallback onSessionExpired;
  final ValueChanged<PracticeSentence> onSelect;
  final VoidCallback onClose;

  @override
  State<SavedSentencesScreen> createState() => _SavedSentencesScreenState();
}

class _SavedSentencesScreenState extends State<SavedSentencesScreen> {
  List<PracticeSentence> sentences = const [];
  bool isLoading = true;
  String? errorText;

  /// 해제 요청을 보낸 문장이다. 서버 응답 전에도 목록에서 빼 손맛을 즉시 준다.
  final Set<int> removingIds = {};

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
      final items = await widget.authService.runAuthenticated(
        (accessToken) => widget.practiceContentApi.fetchSavedSentences(
          accessToken: accessToken,
        ),
      );
      if (mounted) {
        setState(() => sentences = items);
      }
    } on AuthSessionExpiredException {
      widget.onSessionExpired();
    } catch (_) {
      if (mounted) {
        setState(() {
          errorText =
              'Saved sentences could not be loaded. Check your connection '
              'and retry.';
        });
      }
    } finally {
      if (mounted) {
        setState(() => isLoading = false);
      }
    }
  }

  Future<void> toggleSaved(PracticeSentence sentence) async {
    final sentenceId = sentence.sentenceId;
    if (sentenceId == null || removingIds.contains(sentenceId)) {
      return;
    }
    // 해제는 되돌릴 수 있는 동작이라 확인을 묻지 않고 바로 반영한다.
    setState(() => removingIds.add(sentenceId));
    try {
      final saved = await widget.authService.runAuthenticated(
        (accessToken) => widget.practiceContentApi.toggleSavedSentence(
          accessToken: accessToken,
          sentenceId: sentenceId,
        ),
      );
      if (!mounted) {
        return;
      }
      setState(() {
        removingIds.remove(sentenceId);
        if (!saved) {
          sentences = sentences
              .where((item) => item.sentenceId != sentenceId)
              .toList(growable: false);
        }
      });
    } on AuthSessionExpiredException {
      widget.onSessionExpired();
    } catch (_) {
      if (mounted) {
        // 실패하면 목록을 되돌려 실제 저장 상태와 화면을 일치시킨다.
        setState(() => removingIds.remove(sentenceId));
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Could not update this sentence.')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final visible = sentences
        .where((item) => !removingIds.contains(item.sentenceId))
        .toList(growable: false);

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 22),
      children: [
        TopBar(
          title: 'Saved',
          centered: true,
          leading: IconButton(
            key: const ValueKey('saved-sentences-close'),
            tooltip: 'Back',
            onPressed: widget.onClose,
            icon: const Icon(Icons.chevron_left_rounded, size: 26),
          ),
        ),
        const SizedBox(height: 6),
        // 개수는 실제 목록 길이에서 센다. 서버 값을 따로 쓰면 해제 직후 어긋난다.
        Text(
          visible.length == 1 ? '1 sentence' : '${visible.length} sentences',
          style: Theme.of(context).textTheme.bodyMedium,
        ),
        const SizedBox(height: 14),
        if (isLoading)
          const StatePanel(
            icon: Icons.bookmark_border,
            title: 'Loading saved sentences',
            isLoading: true,
          )
        else if (errorText != null)
          StatePanel(
            icon: Icons.wifi_off_outlined,
            title: errorText!,
            actionLabel: 'Retry',
            onAction: load,
          )
        else if (visible.isEmpty)
          const StatePanel(
            icon: Icons.bookmark_border,
            title: 'Nothing saved yet',
            message:
                'Tap the bookmark on a sentence to keep it here for later.',
          )
        else
          Column(
            children: [
              for (var index = 0; index < visible.length; index++)
                SentenceCard(
                  key: ValueKey('saved-sentence-${visible[index].sentenceId}'),
                  sentence: visible[index],
                  onTap: () => widget.onSelect(visible[index]),
                  showDivider: index != visible.length - 1,
                  isSaved: true,
                  onToggleSaved: () => toggleSaved(visible[index]),
                ),
            ],
          ),
      ],
    );
  }
}
