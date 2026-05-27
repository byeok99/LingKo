import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../models/practice_sentence.dart';
import '../widgets/shared_widgets.dart';

class PracticeScreen extends StatefulWidget {
  const PracticeScreen({
    super.key,
    required this.sentence,
    required this.onResult,
    required this.onCustomSentence,
  });

  final PracticeSentence? sentence;
  final VoidCallback onResult;
  final ValueChanged<PracticeSentence> onCustomSentence;

  @override
  State<PracticeScreen> createState() => _PracticeScreenState();
}

class _PracticeScreenState extends State<PracticeScreen> {
  final TextEditingController customSentenceController =
      TextEditingController();
  final FocusNode customSentenceFocusNode = FocusNode();

  bool canSubmitCustomSentence = false;

  @override
  void initState() {
    super.initState();
    customSentenceController.addListener(_syncCustomSentenceState);
    _syncControllerWithSentence();
    _focusEmptyPracticeInput();
  }

  @override
  void didUpdateWidget(covariant PracticeScreen oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (oldWidget.sentence?.text != widget.sentence?.text) {
      _syncControllerWithSentence();
      _focusEmptyPracticeInput();
    }
  }

  @override
  void dispose() {
    customSentenceController.removeListener(_syncCustomSentenceState);
    customSentenceController.dispose();
    customSentenceFocusNode.dispose();
    super.dispose();
  }

  void _focusEmptyPracticeInput() {
    if (widget.sentence != null) {
      return;
    }

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }

      customSentenceFocusNode.requestFocus();
    });
  }

  void _syncCustomSentenceState() {
    final nextCanSubmit = customSentenceController.text.trim().isNotEmpty;

    if (nextCanSubmit == canSubmitCustomSentence) {
      return;
    }

    setState(() {
      canSubmitCustomSentence = nextCanSubmit;
    });
  }

  void _syncControllerWithSentence() {
    final text = widget.sentence?.text ?? '';

    if (customSentenceController.text == text) {
      return;
    }

    customSentenceController.text = text;
    canSubmitCustomSentence = text.trim().isNotEmpty;
  }

  void _submitCustomSentence() {
    final text = customSentenceController.text.trim();

    if (text.isEmpty) {
      return;
    }

    widget.onCustomSentence(PracticeSentence.custom(text));
  }

  @override
  Widget build(BuildContext context) {
    // 아직 실제 녹음 기능은 없습니다.
    // 이 화면은 "듣기 -> 가이드 확인 -> 녹음" 흐름의 레이아웃을 먼저 검증합니다.
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 24),
      children: [
        const TopBar(title: 'Practice'),
        const SizedBox(height: 22),
        _CustomSentenceCard(
          controller: customSentenceController,
          focusNode: customSentenceFocusNode,
          canSubmit: canSubmitCustomSentence,
          onSubmit: _submitCustomSentence,
        ),
        const SizedBox(height: 24),
        if (widget.sentence == null)
          const _EmptyPracticeState()
        else
          _PracticeContent(
            sentence: widget.sentence!,
            onResult: widget.onResult,
          ),
      ],
    );
  }
}

class _PracticeContent extends StatelessWidget {
  const _PracticeContent({required this.sentence, required this.onResult});

  final PracticeSentence sentence;
  final VoidCallback onResult;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(sentence.text, style: Theme.of(context).textTheme.headlineLarge),
        const SizedBox(height: 10),
        Text(
          sentence.pronunciation,
          style: const TextStyle(
            color: AppColors.info,
            fontSize: 22,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 12),
        Text(
          sentence.translation,
          style: Theme.of(context).textTheme.bodyLarge,
        ),
        const SizedBox(height: 24),
        Row(
          children: [
            Expanded(
              child: ActionButton(
                icon: Icons.play_arrow,
                label: 'Normal',
                onPressed: () {},
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: ActionButton(
                icon: Icons.slow_motion_video,
                label: 'Slow',
                onPressed: () {},
              ),
            ),
          ],
        ),
        const SizedBox(height: 28),
        const SectionHeader(title: 'Pronunciation guide'),
        const SizedBox(height: 12),
        Wrap(
          spacing: 10,
          runSpacing: 10,
          children:
              sentence.characters
                  .map((item) => CharacterChip(result: item))
                  .toList(),
        ),
        const SizedBox(height: 28),
        Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: AppColors.surface,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: AppColors.border),
          ),
          child: Row(
            children: [
              const Icon(Icons.lightbulb_outline, color: AppColors.info),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  sentence.point,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 32),
        FilledButton.icon(
          onPressed: onResult,
          icon: const Icon(Icons.mic),
          label: const Text('Record and score'),
          style: FilledButton.styleFrom(
            backgroundColor: AppColors.brandStrong,
            foregroundColor: AppColors.textPrimary,
            minimumSize: const Size.fromHeight(56),
            textStyle: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w800,
            ),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(8),
            ),
          ),
        ),
      ],
    );
  }
}

class _EmptyPracticeState extends StatelessWidget {
  const _EmptyPracticeState();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.border),
      ),
      child: Text(
        'Type a sentence above or choose one from Home.',
        style: Theme.of(context).textTheme.bodyLarge,
      ),
    );
  }
}

class _CustomSentenceCard extends StatelessWidget {
  const _CustomSentenceCard({
    required this.controller,
    required this.focusNode,
    required this.canSubmit,
    required this.onSubmit,
  });

  final TextEditingController controller;
  final FocusNode focusNode;
  final bool canSubmit;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Practice your own sentence',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: 12),
          TextField(
            controller: controller,
            focusNode: focusNode,
            minLines: 1,
            maxLines: 3,
            autofocus: true,
            keyboardType: TextInputType.text,
            textInputAction: TextInputAction.done,
            onSubmitted: (_) => onSubmit(),
            decoration: const InputDecoration(
              hintText: 'Type a Korean sentence',
              prefixIcon: Icon(Icons.edit_outlined),
            ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: canSubmit ? onSubmit : null,
              icon: const Icon(Icons.arrow_forward),
              label: const Text('Use this sentence'),
            ),
          ),
        ],
      ),
    );
  }
}
