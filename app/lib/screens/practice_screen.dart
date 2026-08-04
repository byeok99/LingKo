// 파일 의도: 문장 선택, 표준 발음 확인, 녹음, 자동 평가 제출 흐름을 제공한다.

import 'dart:async';

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../models/evaluation_progress.dart';
import '../models/practice_sentence.dart';
import '../services/audio_recorder_service.dart';
import '../services/sentence_speech_service.dart';
import '../utils/practice_sentence_normalizer.dart';
import '../widgets/evaluation_progress_panel.dart';
import '../widgets/shared_widgets.dart';

const Duration _sentencePreparationDelay = Duration(milliseconds: 700);
const PracticeSentenceInputFormatter _customSentenceInputFormatter =
    PracticeSentenceInputFormatter();

class PracticeScreen extends StatefulWidget {
  const PracticeScreen({
    super.key,
    required this.sentence,
    required this.audioRecorderService,
    required this.sentenceSpeechService,
    required this.onEvaluateRecording,
    required this.onCustomSentence,
    required this.onPrepareCustomSentence,
    required this.remainingPractices,
    required this.evaluationProgress,
    required this.onImmersiveModeChanged,
    required this.onContinueInBackground,
  });

  final PracticeSentence? sentence;
  final AudioRecorderService audioRecorderService;
  final SentenceSpeechService sentenceSpeechService;
  final Future<void> Function(PracticeSentence sentence, String audioPath)
  onEvaluateRecording;
  final ValueChanged<PracticeSentence> onCustomSentence;
  final Future<PracticeSentence> Function(String text) onPrepareCustomSentence;
  final int? remainingPractices;
  final EvaluationProgress evaluationProgress;
  final ValueChanged<bool> onImmersiveModeChanged;
  final VoidCallback onContinueInBackground;

  @override
  State<PracticeScreen> createState() => _PracticeScreenState();
}

class _PracticeScreenState extends State<PracticeScreen> {
  final TextEditingController customSentenceController =
      TextEditingController();
  final FocusNode customSentenceFocusNode = FocusNode();

  bool isPreparingCustomSentence = false;
  String? customSentenceError;
  String? recordedAudioPath;
  bool isRecording = false;
  bool isSubmittingRecording = false;
  String? recordingError;
  String? speechError;
  bool wasPermissionDenied = false;
  Duration recordingDuration = Duration.zero;
  Timer? recordingTimer;
  Timer? sentencePreparationTimer;
  int sentencePreparationRevision = 0;
  bool isSyncingSentenceController = false;

  bool get isPreparedSentenceCurrent {
    final sentence = widget.sentence;
    return sentence != null &&
        normalizePracticeSentenceText(customSentenceController.text) ==
            normalizePracticeSentenceText(sentence.text);
  }

  @override
  void initState() {
    super.initState();
    customSentenceController.addListener(_handleSentenceDraftChanged);
    _syncControllerWithSentence();
  }

  @override
  void didUpdateWidget(covariant PracticeScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.sentence?.text != widget.sentence?.text) {
      sentencePreparationTimer?.cancel();
      sentencePreparationRevision += 1;
      unawaited(_cleanupRecording(reportErrors: false));
      unawaited(widget.sentenceSpeechService.stop());
      _syncControllerWithSentence();
    }
  }

  @override
  void dispose() {
    recordingTimer?.cancel();
    sentencePreparationTimer?.cancel();
    sentencePreparationRevision += 1;
    unawaited(_cleanupRecording(reportErrors: false));
    unawaited(widget.sentenceSpeechService.stop());
    customSentenceController.removeListener(_handleSentenceDraftChanged);
    customSentenceController.dispose();
    customSentenceFocusNode.dispose();
    super.dispose();
  }

  void _handleSentenceDraftChanged() {
    if (isSyncingSentenceController) {
      return;
    }

    sentencePreparationTimer?.cancel();
    final revision = ++sentencePreparationRevision;
    final text = normalizePracticeSentenceText(customSentenceController.text);
    final alreadyPrepared =
        widget.sentence != null &&
        text == normalizePracticeSentenceText(widget.sentence!.text);

    unawaited(_stopSpeechBestEffort());
    setState(() {
      isPreparingCustomSentence = false;
      customSentenceError = null;
      speechError = null;
    });

    if (text.isEmpty || alreadyPrepared) {
      return;
    }

    // 연속 입력마다 API를 호출하지 않고 사용자가 타이핑을 멈춘 문장만 준비한다.
    sentencePreparationTimer = Timer(
      _sentencePreparationDelay,
      () => _prepareSentence(text, revision),
    );
  }

  void _syncControllerWithSentence() {
    final text = normalizePracticeSentenceText(widget.sentence?.text ?? '');
    isSyncingSentenceController = true;
    if (customSentenceController.text != text) {
      customSentenceController.text = text;
    }
    isSyncingSentenceController = false;
    isPreparingCustomSentence = false;
    customSentenceError = null;
    recordedAudioPath = null;
    isRecording = false;
    isSubmittingRecording = false;
    recordingError = null;
    speechError = null;
    wasPermissionDenied = false;
    recordingDuration = Duration.zero;
  }

  Future<void> _prepareSentence(String text, int revision) async {
    if (!_isCurrentPreparation(text, revision)) {
      return;
    }

    setState(() {
      isPreparingCustomSentence = true;
      customSentenceError = null;
    });
    try {
      final prepared = await widget.onPrepareCustomSentence(text);
      if (!_isCurrentPreparation(text, revision)) {
        return;
      }
      setState(() => isPreparingCustomSentence = false);
      widget.onCustomSentence(prepared);
    } catch (_) {
      if (_isCurrentPreparation(text, revision)) {
        setState(() {
          isPreparingCustomSentence = false;
          customSentenceError =
              'We could not prepare this sentence. Check the text and connection, then try again.';
        });
      }
    }
  }

  bool _isCurrentPreparation(String text, int revision) {
    return mounted &&
        revision == sentencePreparationRevision &&
        normalizePracticeSentenceText(customSentenceController.text) == text;
  }

  void _retrySentencePreparation() {
    final text = normalizePracticeSentenceText(customSentenceController.text);
    if (text.isEmpty) {
      return;
    }
    sentencePreparationTimer?.cancel();
    final revision = ++sentencePreparationRevision;
    unawaited(_prepareSentence(text, revision));
  }

  Future<void> _startRecording() async {
    if (widget.sentence == null ||
        !isPreparedSentenceCurrent ||
        isRecording ||
        isSubmittingRecording ||
        widget.evaluationProgress.isActive ||
        widget.remainingPractices == 0) {
      return;
    }
    customSentenceFocusNode.unfocus();
    await _stopSpeechBestEffort();
    if (!mounted) {
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
      widget.onImmersiveModeChanged(true);
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

  Future<void> _speakSentence(SentenceSpeechRate rate) async {
    final sentence = widget.sentence;
    if (sentence == null || !isPreparedSentenceCurrent) {
      return;
    }

    setState(() => speechError = null);
    try {
      // 화면에 표시된 원문을 그대로 읽어 추천·자유 문장의 듣기 기준을 일치시킨다.
      await widget.sentenceSpeechService.speak(sentence.text, rate: rate);
    } catch (_) {
      if (mounted) {
        setState(() {
          speechError =
              'The sentence could not be played. Check the device volume and Korean voice settings.';
        });
      }
    }
  }

  Future<void> _stopSpeechBestEffort() async {
    try {
      await widget.sentenceSpeechService.stop();
    } catch (_) {
      // 녹음 시작과 화면 전환은 기기 TTS 중지 실패 때문에 차단하지 않는다.
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
      } else {
        widget.onImmersiveModeChanged(false);
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          isRecording = false;
          recordingError =
              'Recording could not be saved. Please record the sentence again.';
        });
        widget.onImmersiveModeChanged(false);
      }
    }
  }

  Future<void> _cancelRecording() async {
    await _cleanupRecording(reportErrors: true);
    widget.onImmersiveModeChanged(false);
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
    if (sentence == null ||
        !isPreparedSentenceCurrent ||
        audioPath == null ||
        isSubmittingRecording) {
      return;
    }
    setState(() {
      isSubmittingRecording = true;
      recordingError = null;
    });
    widget.onImmersiveModeChanged(true);
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
    final preparedSentence = isPreparedSentenceCurrent ? widget.sentence : null;
    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 15, 18, 22),
      children: [
        TopBar(
          title:
              widget.evaluationProgress.isActive
                  ? 'Evaluating'
                  : isRecording
                  ? 'Recording'
                  : 'Practice',
          centered: true,
        ),
        const SizedBox(height: 8),
        if (widget.evaluationProgress.isActive ||
            widget.evaluationProgress.stage == EvaluationProgressStage.failed)
          EvaluationProgressPanel(
            progress: widget.evaluationProgress,
            onRetry: recordedAudioPath == null ? null : _submitRecording,
            onContinueInBackground: widget.onContinueInBackground,
          )
        else if (isRecording)
          _RecordingView(
            sentence: widget.sentence!,
            duration: recordingDuration,
            onStop: _stopRecording,
            onCancel: _cancelRecording,
          )
        else ...[
          _SentenceComposerCard(
            controller: customSentenceController,
            focusNode: customSentenceFocusNode,
            preparedSentence: preparedSentence,
            isLoading: isPreparingCustomSentence,
            errorText: customSentenceError,
            onRetry: _retrySentencePreparation,
          ),
          if (preparedSentence != null) ...[
            const SizedBox(height: 24),
            _PracticeContent(
              remainingPractices: widget.remainingPractices,
              isSubmitting: isSubmittingRecording,
              errorText: recordingError,
              speechErrorText: speechError,
              wasPermissionDenied: wasPermissionDenied,
              hasRecording: recordedAudioPath != null,
              onStartRecording: _startRecording,
              onRetryRecording: _submitRecording,
              onDiscardRecording: _cancelRecording,
              onPlayNormal: () => _speakSentence(SentenceSpeechRate.normal),
              onPlaySlow: () => _speakSentence(SentenceSpeechRate.slow),
            ),
          ],
        ],
      ],
    );
  }
}

class _PracticeContent extends StatelessWidget {
  const _PracticeContent({
    required this.remainingPractices,
    required this.isSubmitting,
    required this.errorText,
    required this.speechErrorText,
    required this.wasPermissionDenied,
    required this.hasRecording,
    required this.onStartRecording,
    required this.onRetryRecording,
    required this.onDiscardRecording,
    required this.onPlayNormal,
    required this.onPlaySlow,
  });

  final int? remainingPractices;
  final bool isSubmitting;
  final String? errorText;
  final String? speechErrorText;
  final bool wasPermissionDenied;
  final bool hasRecording;
  final VoidCallback onStartRecording;
  final VoidCallback onRetryRecording;
  final VoidCallback onDiscardRecording;
  final VoidCallback onPlayNormal;
  final VoidCallback onPlaySlow;

  @override
  Widget build(BuildContext context) {
    final quotaExhausted =
        remainingPractices != null && remainingPractices! <= 0;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SectionHeader(title: 'Listen'),
        const SizedBox(height: 10),
        Row(
          children: [
            Expanded(
              child: ActionButton(
                key: const ValueKey('play-sentence-normal'),
                icon: Icons.play_arrow,
                label: 'Normal',
                onPressed: onPlayNormal,
              ),
            ),
            const SizedBox(width: 9),
            Expanded(
              child: ActionButton(
                key: const ValueKey('play-sentence-slow'),
                icon: Icons.slow_motion_video,
                label: 'Slow',
                onPressed: onPlaySlow,
              ),
            ),
          ],
        ),
        if (speechErrorText != null) ...[
          const SizedBox(height: AppSpacing.lg),
          StatePanel(
            icon: Icons.volume_off_outlined,
            title: 'Audio playback needs attention',
            message: speechErrorText!,
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
        const SizedBox(height: 13),
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
                    ? 'No evaluation chances available'
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
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
          child: Column(
            children: [
              Text(
                sentence.text,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.headlineMedium,
              ),
            ],
          ),
        ),
        const SizedBox(height: 20),
        Semantics(
          label: 'Recording duration 0 minutes $seconds seconds',
          child: SizedBox.square(
            dimension: 194,
            child: Stack(
              alignment: Alignment.center,
              children: [
                const SizedBox.square(
                  dimension: 194,
                  child: CircularProgressIndicator(
                    value: 0.38,
                    strokeWidth: 13,
                    backgroundColor: Color(0xFFE7EEF4),
                    color: AppColors.primary,
                  ),
                ),
                Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      '0:$seconds',
                      style: const TextStyle(
                        fontSize: 40,
                        fontWeight: FontWeight.w900,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 5),
                    const Text(
                      '/ 10 sec',
                      style: TextStyle(
                        color: AppColors.textSecondary,
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 14),
        const _RecordingWaveform(),
        const SizedBox(height: 17),
        const Text(
          'Speak naturally. Stop when you finish the full sentence.',
          textAlign: TextAlign.center,
          style: TextStyle(color: AppColors.textSecondary, fontSize: 12),
        ),
        const SizedBox(height: 34),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            _RecordingControl(
              label: 'Cancel recording',
              icon: Icons.close,
              onTap: onCancel,
            ),
            _RecordingControl(
              label: 'Stop and analyze',
              icon: Icons.stop_rounded,
              primary: true,
              onTap: onStop,
            ),
          ],
        ),
      ],
    );
  }
}

class _RecordingControl extends StatelessWidget {
  const _RecordingControl({
    required this.label,
    required this.icon,
    required this.onTap,
    this.primary = false,
  });

  final String label;
  final IconData icon;
  final VoidCallback onTap;
  final bool primary;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: label,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppSizes.pillRadius),
        child: Padding(
          padding: const EdgeInsets.all(8),
          child: Column(
            children: [
              Container(
                width: primary ? 62 : 46,
                height: primary ? 62 : 46,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: primary ? AppColors.primary : AppColors.card,
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: primary ? AppColors.blue200 : AppColors.border,
                    width: primary ? 5 : 1,
                  ),
                ),
                child: Icon(
                  icon,
                  color: primary ? AppColors.card : AppColors.textPrimary,
                  size: primary ? 24 : 20,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                label,
                style: const TextStyle(
                  color: AppColors.textPrimary,
                  fontSize: 11,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ],
          ),
        ),
      ),
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

class _SentenceComposerCard extends StatelessWidget {
  const _SentenceComposerCard({
    required this.controller,
    required this.focusNode,
    required this.preparedSentence,
    required this.isLoading,
    required this.errorText,
    required this.onRetry,
  });

  final TextEditingController controller;
  final FocusNode focusNode;
  final PracticeSentence? preparedSentence;
  final bool isLoading;
  final String? errorText;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      color: AppColors.blue50,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'Practice sentence',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: AppSpacing.md),
          TextField(
            key: const ValueKey('practice-sentence-field'),
            controller: controller,
            focusNode: focusNode,
            // 문장부호·기호가 평가 글자로 전달되지 않도록 입력과 붙여넣기 단계에서 즉시 제거한다.
            inputFormatters: [_customSentenceInputFormatter],
            minLines: 1,
            maxLines: 4,
            textInputAction: TextInputAction.done,
            onSubmitted: (_) => focusNode.unfocus(),
            decoration: const InputDecoration(
              hintText: 'Type a Korean sentence',
              prefixIcon: Icon(Icons.edit_outlined),
            ),
          ),
          if (isLoading) ...[
            const SizedBox(height: AppSpacing.md),
            const _SentencePreparationStatus(
              icon: SizedBox.square(
                dimension: 16,
                child: CircularProgressIndicator(strokeWidth: 2),
              ),
              text: 'Preparing standard pronunciation…',
            ),
          ] else if (errorText != null) ...[
            const SizedBox(height: AppSpacing.md),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _SentencePreparationStatus(
                  icon: const Icon(
                    Icons.error_outline,
                    size: 18,
                    color: AppColors.error,
                  ),
                  text: errorText!,
                  textColor: AppColors.error,
                ),
                Align(
                  alignment: Alignment.centerRight,
                  child: TextButton.icon(
                    onPressed: onRetry,
                    icon: const Icon(Icons.refresh, size: 17),
                    label: const Text('Retry'),
                  ),
                ),
              ],
            ),
          ],
          if (preparedSentence != null &&
              preparedSentence!.pronunciation.trim().isNotEmpty) ...[
            const SizedBox(height: AppSpacing.md),
            Container(
              key: const ValueKey('practice-standard-pronunciation'),
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 11),
              decoration: BoxDecoration(
                color: AppColors.card,
                border: Border.all(color: AppColors.blue200),
                borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
              ),
              child: Column(
                children: [
                  const Text(
                    'Standard pronunciation',
                    style: TextStyle(
                      color: AppColors.textSecondary,
                      fontSize: 10,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    preparedSentence!.pronunciation,
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      color: AppColors.primaryDark,
                      fontSize: 17,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _SentencePreparationStatus extends StatelessWidget {
  const _SentencePreparationStatus({
    required this.icon,
    required this.text,
    this.textColor = AppColors.textSecondary,
  });

  final Widget icon;
  final String text;
  final Color textColor;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        icon,
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            style: TextStyle(
              color: textColor,
              fontSize: 12,
              fontWeight: FontWeight.w700,
              height: 1.35,
            ),
          ),
        ),
      ],
    );
  }
}
