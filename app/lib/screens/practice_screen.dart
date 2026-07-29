// 파일 의도: 문장 선택, 표준 발음 확인, 녹음, 자동 평가 제출 흐름을 제공한다.

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app/app_theme.dart';
import '../models/evaluation_progress.dart';
import '../models/practice_sentence.dart';
import '../services/audio_recorder_service.dart';
import '../widgets/evaluation_progress_panel.dart';
import '../widgets/shared_widgets.dart';

final RegExp _customSentenceSpecialCharacterPattern = RegExp(
  r'[\p{P}\p{S}]',
  unicode: true,
);
final TextInputFormatter _customSentenceSpecialCharacterFormatter =
    FilteringTextInputFormatter.deny(_customSentenceSpecialCharacterPattern);

String _normalizeCustomSentence(String value) {
  return value.replaceAll(_customSentenceSpecialCharacterPattern, '').trim();
}

class PracticeScreen extends StatefulWidget {
  const PracticeScreen({
    super.key,
    required this.sentence,
    required this.audioRecorderService,
    required this.onEvaluateRecording,
    required this.onCustomSentence,
    required this.onPrepareCustomSentence,
    required this.remainingPractices,
    required this.evaluationProgress,
  });

  final PracticeSentence? sentence;
  final AudioRecorderService audioRecorderService;
  final Future<void> Function(PracticeSentence sentence, String audioPath)
  onEvaluateRecording;
  final ValueChanged<PracticeSentence> onCustomSentence;
  final Future<PracticeSentence> Function(String text) onPrepareCustomSentence;
  final int? remainingPractices;
  final EvaluationProgress evaluationProgress;

  @override
  State<PracticeScreen> createState() => _PracticeScreenState();
}

class _PracticeScreenState extends State<PracticeScreen> {
  final TextEditingController customSentenceController =
      TextEditingController();
  final FocusNode customSentenceFocusNode = FocusNode();

  bool customMode = false;
  bool canSubmitCustomSentence = false;
  bool isPreparingCustomSentence = false;
  String? customSentenceError;
  String? recordedAudioPath;
  bool isRecording = false;
  bool isSubmittingRecording = false;
  String? recordingError;
  bool wasPermissionDenied = false;
  Duration recordingDuration = Duration.zero;
  Timer? recordingTimer;

  @override
  void initState() {
    super.initState();
    customMode = widget.sentence == null;
    customSentenceController.addListener(_syncCustomSentenceState);
    _syncControllerWithSentence();
  }

  @override
  void didUpdateWidget(covariant PracticeScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.sentence?.text != widget.sentence?.text) {
      unawaited(_cleanupRecording(reportErrors: false));
      _syncControllerWithSentence();
      customMode = widget.sentence?.source == 'CUSTOM';
    }
  }

  @override
  void dispose() {
    recordingTimer?.cancel();
    unawaited(_cleanupRecording(reportErrors: false));
    customSentenceController.removeListener(_syncCustomSentenceState);
    customSentenceController.dispose();
    customSentenceFocusNode.dispose();
    super.dispose();
  }

  void _syncCustomSentenceState() {
    final next =
        _normalizeCustomSentence(customSentenceController.text).isNotEmpty;
    if (next != canSubmitCustomSentence) {
      setState(() => canSubmitCustomSentence = next);
    }
  }

  void _syncControllerWithSentence() {
    final text = widget.sentence?.text ?? '';
    if (customSentenceController.text != text) {
      customSentenceController.text = text;
    }
    canSubmitCustomSentence = text.trim().isNotEmpty;
    recordedAudioPath = null;
    isRecording = false;
    isSubmittingRecording = false;
    recordingError = null;
    wasPermissionDenied = false;
    recordingDuration = Duration.zero;
  }

  Future<void> _submitCustomSentence() async {
    final text = _normalizeCustomSentence(customSentenceController.text);
    if (text.isEmpty || isPreparingCustomSentence) {
      return;
    }
    setState(() {
      isPreparingCustomSentence = true;
      customSentenceError = null;
    });
    try {
      final prepared = await widget.onPrepareCustomSentence(text);
      if (mounted) {
        widget.onCustomSentence(prepared);
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          customSentenceError =
              'We could not prepare this sentence. Check the text and connection, then try again.';
        });
      }
    } finally {
      if (mounted) {
        setState(() => isPreparingCustomSentence = false);
      }
    }
  }

  Future<void> _startRecording() async {
    if (widget.sentence == null ||
        isRecording ||
        isSubmittingRecording ||
        widget.evaluationProgress.isActive ||
        widget.remainingPractices == 0) {
      return;
    }
    setState(() {
      recordingError = null;
      recordedAudioPath = null;
      recordingDuration = Duration.zero;
    });
    try {
      final hasPermission = await widget.audioRecorderService.hasPermission();
      if (!hasPermission) {
        if (mounted) {
          setState(() {
            wasPermissionDenied = true;
            recordingError =
                'Microphone access is required. Allow access in device settings, then retry.';
          });
        }
        return;
      }
      await widget.audioRecorderService.start();
      if (!mounted) {
        return;
      }
      setState(() {
        wasPermissionDenied = false;
        isRecording = true;
      });
      recordingTimer = Timer.periodic(const Duration(seconds: 1), (_) {
        if (mounted) {
          setState(() {
            recordingDuration += const Duration(seconds: 1);
          });
        }
      });
    } catch (_) {
      if (mounted) {
        setState(() {
          recordingError =
              'Recording could not start. Check microphone access and available storage.';
        });
      }
    }
  }

  Future<void> _stopRecording() async {
    if (!isRecording) {
      return;
    }
    recordingTimer?.cancel();
    try {
      final path = await widget.audioRecorderService.stop();
      if (!mounted) {
        return;
      }
      setState(() {
        isRecording = false;
        recordedAudioPath = path;
        recordingError =
            path == null
                ? 'No recording file was created. Please record again.'
                : null;
      });
      if (path != null) {
        await _submitRecording();
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          isRecording = false;
          recordingError =
              'Recording could not be saved. Please record the sentence again.';
        });
      }
    }
  }

  Future<void> _cancelRecording() async {
    await _cleanupRecording(reportErrors: true);
  }

  Future<void> _cleanupRecording({required bool reportErrors}) async {
    recordingTimer?.cancel();
    final shouldCancel = isRecording;
    final audioPath = recordedAudioPath;
    try {
      if (shouldCancel) {
        await widget.audioRecorderService.cancel();
      }
      if (audioPath != null) {
        await widget.audioRecorderService.delete(audioPath);
      }
    } catch (_) {
      if (reportErrors && mounted) {
        setState(() {
          recordingError =
              'The temporary recording could not be cleared. You can continue safely.';
        });
      }
      return;
    }
    if (mounted && reportErrors) {
      setState(() {
        isRecording = false;
        recordedAudioPath = null;
        recordingError = null;
        recordingDuration = Duration.zero;
        wasPermissionDenied = false;
      });
    }
  }

  Future<void> _submitRecording() async {
    final sentence = widget.sentence;
    final audioPath = recordedAudioPath;
    if (sentence == null || audioPath == null || isSubmittingRecording) {
      return;
    }
    setState(() {
      isSubmittingRecording = true;
      recordingError = null;
    });
    try {
      await widget.onEvaluateRecording(sentence, audioPath);
      await _deleteRecordingBestEffort(audioPath);
      if (mounted) {
        setState(() => recordedAudioPath = null);
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          recordingError =
              'The evaluation could not be completed. You can retry with this recording.';
        });
      }
    } finally {
      if (mounted) {
        setState(() => isSubmittingRecording = false);
      }
    }
  }

  Future<void> _deleteRecordingBestEffort(String audioPath) async {
    try {
      await widget.audioRecorderService.delete(audioPath);
    } catch (_) {
      // 평가 결과가 생성된 뒤의 임시 파일 정리 실패는 결과 표시를 막지 않는다.
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(
        AppSpacing.xl,
        AppSpacing.lg,
        AppSpacing.xl,
        AppSpacing.section,
      ),
      children: [
        TopBar(
          title: widget.evaluationProgress.isActive ? 'Evaluating' : 'Practice',
          subtitle:
              widget.evaluationProgress.isActive
                  ? 'Your current job is saved in this app session.'
                  : 'Practice the whole sentence in one recording.',
        ),
        const SizedBox(height: AppSpacing.xxl),
        if (widget.evaluationProgress.isActive ||
            widget.evaluationProgress.stage == EvaluationProgressStage.failed)
          EvaluationProgressPanel(
            progress: widget.evaluationProgress,
            onRetry: recordedAudioPath == null ? null : _submitRecording,
          )
        else if (isRecording)
          _RecordingView(
            sentence: widget.sentence!,
            duration: recordingDuration,
            onStop: _stopRecording,
            onCancel: _cancelRecording,
          )
        else ...[
          _ModeSelector(
            customMode: customMode,
            onChanged: (nextCustomMode) {
              setState(() => customMode = nextCustomMode);
              if (nextCustomMode) {
                customSentenceFocusNode.requestFocus();
              }
            },
          ),
          const SizedBox(height: AppSpacing.lg),
          if (customMode)
            _CustomSentenceCard(
              controller: customSentenceController,
              focusNode: customSentenceFocusNode,
              canSubmit: canSubmitCustomSentence && !isPreparingCustomSentence,
              isLoading: isPreparingCustomSentence,
              errorText: customSentenceError,
              onSubmit: _submitCustomSentence,
            ),
          if (customMode) const SizedBox(height: AppSpacing.lg),
          if (widget.sentence == null)
            const StatePanel(
              icon: Icons.text_fields,
              title: 'Choose or enter a sentence',
              message:
                  'Select a recommendation on Home or prepare your own Korean sentence.',
            )
          else
            _PracticeContent(
              sentence: widget.sentence!,
              remainingPractices: widget.remainingPractices,
              isSubmitting: isSubmittingRecording,
              errorText: recordingError,
              wasPermissionDenied: wasPermissionDenied,
              hasRecording: recordedAudioPath != null,
              onStartRecording: _startRecording,
              onRetryRecording: _submitRecording,
              onDiscardRecording: _cancelRecording,
            ),
        ],
      ],
    );
  }
}

class _ModeSelector extends StatelessWidget {
  const _ModeSelector({required this.customMode, required this.onChanged});

  final bool customMode;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return SegmentedButton<bool>(
      segments: const [
        ButtonSegment(
          value: false,
          icon: Icon(Icons.auto_awesome_outlined),
          label: Text('Recommended'),
        ),
        ButtonSegment(
          value: true,
          icon: Icon(Icons.edit_outlined),
          label: Text('My sentence'),
        ),
      ],
      selected: {customMode},
      onSelectionChanged: (selection) => onChanged(selection.first),
      showSelectedIcon: false,
    );
  }
}

class _PracticeContent extends StatelessWidget {
  const _PracticeContent({
    required this.sentence,
    required this.remainingPractices,
    required this.isSubmitting,
    required this.errorText,
    required this.wasPermissionDenied,
    required this.hasRecording,
    required this.onStartRecording,
    required this.onRetryRecording,
    required this.onDiscardRecording,
  });

  final PracticeSentence sentence;
  final int? remainingPractices;
  final bool isSubmitting;
  final String? errorText;
  final bool wasPermissionDenied;
  final bool hasRecording;
  final VoidCallback onStartRecording;
  final VoidCallback onRetryRecording;
  final VoidCallback onDiscardRecording;

  @override
  Widget build(BuildContext context) {
    final quotaExhausted =
        remainingPractices != null && remainingPractices! <= 0;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        AppCard(
          child: Column(
            children: [
              Text(
                sentence.text,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.headlineMedium,
              ),
              const SizedBox(height: AppSpacing.md),
              Text(
                sentence.pronunciation,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  color: AppColors.primaryDark,
                  fontSize: 17,
                  fontWeight: FontWeight.w700,
                ),
              ),
              if (sentence.translation.isNotEmpty) ...[
                const SizedBox(height: AppSpacing.sm),
                Text(
                  sentence.translation,
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.xxl),
        const SectionHeader(title: 'Listen'),
        const SizedBox(height: AppSpacing.md),
        const Row(
          children: [
            Expanded(
              child: ActionButton(
                icon: Icons.play_arrow,
                label: 'Normal · Coming soon',
              ),
            ),
            SizedBox(width: AppSpacing.md),
            Expanded(
              child: ActionButton(
                icon: Icons.slow_motion_video,
                label: 'Slow · Coming soon',
              ),
            ),
          ],
        ),
        const SizedBox(height: AppSpacing.xxl),
        const SectionHeader(title: 'Pronunciation guide'),
        const SizedBox(height: AppSpacing.md),
        if (sentence.characters.isEmpty)
          Text(
            'Pronunciation guide details are not available for this sentence.',
            style: Theme.of(context).textTheme.bodyMedium,
          )
        else
          Wrap(
            spacing: AppSpacing.sm,
            runSpacing: AppSpacing.sm,
            children: [
              for (final character in sentence.characters)
                CharacterChip(result: character),
            ],
          ),
        if (sentence.point.isNotEmpty) ...[
          const SizedBox(height: AppSpacing.xxl),
          AppCard(
            color: AppColors.softBlue,
            child: Row(
              children: [
                const Icon(Icons.lightbulb_outline, color: AppColors.primary),
                const SizedBox(width: AppSpacing.md),
                Expanded(child: Text(sentence.point)),
              ],
            ),
          ),
        ],
        if (errorText != null) ...[
          const SizedBox(height: AppSpacing.lg),
          StatePanel(
            icon:
                wasPermissionDenied
                    ? Icons.mic_off_outlined
                    : Icons.error_outline,
            title:
                wasPermissionDenied
                    ? 'Microphone access is off'
                    : 'Recording needs attention',
            message: errorText,
          ),
        ],
        const SizedBox(height: AppSpacing.xxl),
        if (hasRecording) ...[
          PrimaryButton(
            label: 'Retry with this recording',
            icon: Icons.refresh,
            isLoading: isSubmitting,
            onPressed: onRetryRecording,
          ),
          const SizedBox(height: AppSpacing.sm),
          SecondaryButton(
            label: 'Record again',
            icon: Icons.mic_none,
            onPressed: isSubmitting ? null : onDiscardRecording,
          ),
        ] else
          PrimaryButton(
            key: const ValueKey('record-primary'),
            label:
                quotaExhausted
                    ? 'No practices left today'
                    : wasPermissionDenied
                    ? 'Retry microphone permission'
                    : 'Start recording',
            icon: Icons.mic,
            onPressed: quotaExhausted ? null : onStartRecording,
          ),
      ],
    );
  }
}

class _RecordingView extends StatelessWidget {
  const _RecordingView({
    required this.sentence,
    required this.duration,
    required this.onStop,
    required this.onCancel,
  });

  final PracticeSentence sentence;
  final Duration duration;
  final VoidCallback onStop;
  final VoidCallback onCancel;

  @override
  Widget build(BuildContext context) {
    final seconds = duration.inSeconds.toString().padLeft(2, '0');
    return Column(
      children: [
        AppCard(
          child: Text(
            sentence.text,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.headlineMedium,
          ),
        ),
        const SizedBox(height: AppSpacing.section),
        const StatusBadge(label: 'Recording', tone: StatusTone.error),
        const SizedBox(height: AppSpacing.lg),
        Semantics(
          label: 'Recording duration 0 minutes $seconds seconds',
          child: Text(
            '0:$seconds',
            style: const TextStyle(
              fontSize: 52,
              fontWeight: FontWeight.w900,
              color: AppColors.textPrimary,
            ),
          ),
        ),
        const SizedBox(height: AppSpacing.xxl),
        const _RecordingWaveform(),
        const SizedBox(height: AppSpacing.section),
        PrimaryButton(
          label: 'Stop and analyze',
          icon: Icons.stop,
          onPressed: onStop,
        ),
        const SizedBox(height: AppSpacing.sm),
        SecondaryButton(
          label: 'Cancel recording',
          icon: Icons.close,
          onPressed: onCancel,
        ),
      ],
    );
  }
}

class _RecordingWaveform extends StatelessWidget {
  const _RecordingWaveform();

  @override
  Widget build(BuildContext context) {
    const heights = [18.0, 32.0, 48.0, 26.0, 40.0, 58.0, 34.0, 46.0, 24.0];
    return Semantics(
      label: 'Microphone is actively recording',
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          for (final height in heights)
            Container(
              width: 5,
              height: height,
              margin: const EdgeInsets.symmetric(horizontal: 3),
              decoration: BoxDecoration(
                color: AppColors.primary,
                borderRadius: BorderRadius.circular(AppSizes.pillRadius),
              ),
            ),
        ],
      ),
    );
  }
}

class _CustomSentenceCard extends StatelessWidget {
  const _CustomSentenceCard({
    required this.controller,
    required this.focusNode,
    required this.canSubmit,
    required this.isLoading,
    required this.errorText,
    required this.onSubmit,
  });

  final TextEditingController controller;
  final FocusNode focusNode;
  final bool canSubmit;
  final bool isLoading;
  final String? errorText;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'Practice your own sentence',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: AppSpacing.md),
          TextField(
            controller: controller,
            focusNode: focusNode,
            // 문장부호·기호가 평가 글자로 전달되지 않도록 입력과 붙여넣기 단계에서 즉시 제거한다.
            inputFormatters: [_customSentenceSpecialCharacterFormatter],
            minLines: 1,
            maxLines: 4,
            textInputAction: TextInputAction.done,
            onSubmitted: (_) => onSubmit(),
            decoration: const InputDecoration(
              hintText: 'Type a Korean sentence',
              prefixIcon: Icon(Icons.edit_outlined),
            ),
          ),
          if (errorText != null) ...[
            const SizedBox(height: AppSpacing.sm),
            Text(
              errorText!,
              style: const TextStyle(
                color: AppColors.error,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
          const SizedBox(height: AppSpacing.md),
          PrimaryButton(
            label: 'Use this sentence',
            icon: Icons.arrow_forward,
            isLoading: isLoading,
            onPressed: canSubmit ? onSubmit : null,
          ),
        ],
      ),
    );
  }
}
