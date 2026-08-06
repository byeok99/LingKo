// 파일 의도: 문장 선택, 표준 발음 확인, 녹음, 자동 평가 제출 흐름을 제공한다.

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import '../models/evaluation_progress.dart';
import '../models/practice_sentence.dart';
import '../services/audio_recorder_service.dart';
import '../services/sentence_speech_service.dart';
import '../utils/practice_sentence_normalizer.dart';
import '../widgets/evaluation_progress_panel.dart';
import '../widgets/romanized_pronunciation.dart';
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
  /// 화면에 표시하는 상한과 실제 종료 시점을 같은 값에서 가져와 표시와 동작을 일치시킨다.
  static const maximumRecordingDuration = Duration(seconds: 10);

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
  StreamSubscription<double>? amplitudeSubscription;
  double currentAmplitude = 0;
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
    amplitudeSubscription?.cancel();
    amplitudeSubscription = null;
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
      // 녹음은 화면을 보지 않고 수행하는 동작이라 시작·종료를 촉각으로 알린다.
      unawaited(HapticFeedback.mediumImpact());
      setState(() {
        wasPermissionDenied = false;
        isRecording = true;
      });
      widget.onImmersiveModeChanged(true);
      // 링과 숫자가 같은 값을 쓰도록 경과 시간을 한 곳에서만 센다.
      // 100ms 간격이면 진행 링이 끊겨 보이지 않으면서 갱신 비용도 크지 않다.
      recordingTimer = Timer.periodic(const Duration(milliseconds: 100), (_) {
        if (!mounted) {
          return;
        }
        final next = recordingDuration + const Duration(milliseconds: 100);
        setState(() => recordingDuration = next);
        // 상한을 넘기면 안내만 하지 않고 실제로 멈춰야 표시와 동작이 일치한다.
        if (next >= maximumRecordingDuration) {
          unawaited(_stopRecording());
        }
      });
      amplitudeSubscription = widget.audioRecorderService
          .amplitudeStream()
          .listen(
            (level) {
              if (mounted) {
                setState(() => currentAmplitude = level);
              }
            },
            onError: (_) {
              // 레벨 표시는 보조 정보이므로 실패해도 녹음 자체는 계속 진행한다.
            },
          );
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
    amplitudeSubscription?.cancel();
    amplitudeSubscription = null;
    unawaited(HapticFeedback.mediumImpact());
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

  /// 현재 파일을 평가에 보내지 않고 버린 뒤 같은 문장을 즉시 다시 녹음한다.
  Future<void> _restartRecording() async {
    await _cleanupRecording(reportErrors: true);
    if (mounted && !isRecording) {
      await _startRecording();
    }
  }

  Future<void> _cleanupRecording({required bool reportErrors}) async {
    recordingTimer?.cancel();
    amplitudeSubscription?.cancel();
    amplitudeSubscription = null;
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
      padding: const EdgeInsets.fromLTRB(18, 16, 18, 22),
      children: [
        TopBar(
          title:
              widget.evaluationProgress.isActive
                  ? 'Scoring'
                  : isRecording
                  ? 'Recording'
                  : 'Practice',
          centered: widget.evaluationProgress.isActive || isRecording,
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
            maximumDuration: maximumRecordingDuration,
            amplitude: currentAmplitude,
            onStop: _stopRecording,
            onCancel: _cancelRecording,
            onRestart: _restartRecording,
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
        const SizedBox(height: 14),
        // Normal과 Slow는 동등한 선택지라 같은 무게의 secondary로 나란히 둔다.
        // 한쪽을 채우면 그쪽이 정답처럼 보인다.
        Row(
          children: [
            Expanded(
              child: SecondaryButton(
                key: const ValueKey('play-sentence-normal'),
                label: 'Normal',
                onPressed: onPlayNormal,
              ),
            ),
            const SizedBox(width: 9),
            Expanded(
              child: SecondaryButton(
                key: const ValueKey('play-sentence-slow'),
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
            isLoading: isSubmitting,
            onPressed: onRetryRecording,
          ),
          const SizedBox(height: AppSpacing.sm),
          SecondaryButton(
            label: 'Record again',
            onPressed: isSubmitting ? null : onDiscardRecording,
          ),
        ] else
          PrimaryButton(
            key: const ValueKey('record-primary'),
            // 남은 기회가 없으면 형태는 그대로 두고 채움만 약화한다(비활성).
            // 라벨로 이유를 말하되 버튼을 없애면 어디서 다시 시작할지 알 수 없다.
            label:
                quotaExhausted
                    ? 'No practices left'
                    : wasPermissionDenied
                    ? 'Retry microphone permission'
                    : 'Record',
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
    required this.maximumDuration,
    required this.amplitude,
    required this.onStop,
    required this.onCancel,
    required this.onRestart,
  });

  final PracticeSentence sentence;
  final Duration duration;
  final Duration maximumDuration;
  final double amplitude;
  final VoidCallback onStop;
  final VoidCallback onCancel;
  final VoidCallback onRestart;

  @override
  Widget build(BuildContext context) {
    final elapsed = duration.inSeconds;
    final minutes = elapsed ~/ 60;
    final seconds = (elapsed % 60).toString().padLeft(2, '0');
    final progress =
        maximumDuration.inMilliseconds == 0
            ? 0.0
            : (duration.inMilliseconds / maximumDuration.inMilliseconds).clamp(
              0.0,
              1.0,
            );
    return Column(
      children: [
        AppCard(
          color: context.palette.blue50,
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
          child: Column(
            children: [
              Text(
                sentence.text,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.headlineMedium,
              ),
              if (sentence.romanizedPronunciation.trim().isNotEmpty) ...[
                const SizedBox(height: 6),
                RomanizedPronunciation(
                  key: const ValueKey('recording-romanized-pronunciation'),
                  text: sentence.romanizedPronunciation,
                  textAlign: TextAlign.center,
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 20),
        Semantics(
          label: 'Recording duration $minutes minutes $seconds seconds',
          child: SizedBox.square(
            dimension: 194,
            child: Stack(
              alignment: Alignment.center,
              children: [
                SizedBox.square(
                  dimension: 194,
                  // 실제 경과 비율을 그린다. 고정값을 쓰면 멈춘 것처럼 보인다.
                  child: CircularProgressIndicator(
                    value: progress,
                    strokeWidth: 13,
                    backgroundColor: const Color(0xFFE7EEF4),
                    color:
                        progress >= 1
                            ? context.palette.warning
                            : context.palette.primary,
                  ),
                ),
                Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      '$minutes:$seconds',
                      style: TextStyle(
                        fontSize: 40,
                        fontWeight: FontWeight.w900,
                        color: context.palette.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 5),
                    Text(
                      '/ ${maximumDuration.inSeconds} sec',
                      style: TextStyle(
                        color: context.palette.textSecondary,
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
        _RecordingWaveform(amplitude: amplitude),
        // 말하는 순간에 읽으라고 문장을 두지 않는다. 사용자는 화면이 아니라
        // 발음에 집중해야 한다.
        const SizedBox(height: 34),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            _RecordingControl(
              label: 'Cancel',
              semanticLabel: 'Cancel recording',
              icon: Icons.close,
              onTap: onCancel,
            ),
            _RecordingControl(
              label: 'Stop',
              semanticLabel: 'Stop and analyze',
              icon: Icons.stop_rounded,
              primary: true,
              onTap: onStop,
            ),
            _RecordingControl(
              label: 'Restart',
              icon: Icons.refresh_rounded,
              onTap: onRestart,
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
    this.semanticLabel,
    required this.icon,
    required this.onTap,
    this.primary = false,
  });

  final String label;
  final String? semanticLabel;
  final IconData icon;
  final VoidCallback onTap;
  final bool primary;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: semanticLabel ?? label,
      excludeSemantics: true,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppSizes.pillRadius),
        child: Padding(
          padding: const EdgeInsets.all(8),
          child: Column(
            children: [
              Container(
                width: primary ? 68 : 48,
                height: primary ? 68 : 48,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  // 녹음 화면의 핵심 행동은 파란 원으로 다른 보조 제어와 구분한다.
                  color:
                      primary ? context.palette.primary : context.palette.card,
                  shape: BoxShape.circle,
                  border: Border.all(color: context.palette.borderStrong),
                ),
                child:
                    primary
                        // 정지는 아이콘이 아니라 사각형이라 색을 보지 않아도 구분된다.
                        ? Container(
                          width: 24,
                          height: 24,
                          decoration: BoxDecoration(
                            color: context.palette.onPrimary,
                            borderRadius: BorderRadius.circular(6),
                          ),
                        )
                        : Icon(
                          icon,
                          color: context.palette.textPrimary,
                          size: 20,
                        ),
              ),
              const SizedBox(height: 6),
              Text(
                label,
                style: TextStyle(
                  color: context.palette.textSecondary,
                  fontSize: 11,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 마이크 입력 레벨을 막대 높이로 보여준다.
///
/// 고정 높이 막대는 마이크가 죽어 있어도 똑같이 보여서, 사용자가 녹음 실패를
/// 평가 기회를 쓰고 결과를 기다린 뒤에야 알게 된다. 실제 레벨에 반응해야
/// "지금 소리가 들어오고 있다"를 녹음 중에 확인할 수 있다.
class _RecordingWaveform extends StatelessWidget {
  const _RecordingWaveform({required this.amplitude});

  /// 0.0(무음)~1.0(최대) 범위의 현재 입력 레벨이다.
  final double amplitude;

  static const _barCount = 9;
  static const _minHeight = 6.0;
  static const _maxHeight = 58.0;

  @override
  Widget build(BuildContext context) {
    final level = amplitude.clamp(0.0, 1.0);
    final isSilent = level <= 0.02;
    return Semantics(
      // 막대 자체에는 읽을 내용이 없으므로 이 묶음이 하나의 안내 노드가 되게 한다.
      container: true,
      label:
          isSilent
              ? 'No sound is being picked up'
              : 'Microphone level ${(level * 100).round()} percent',
      child: SizedBox(
        height: _maxHeight,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            for (var index = 0; index < _barCount; index++)
              AnimatedContainer(
                duration: const Duration(milliseconds: 120),
                curve: Curves.easeOut,
                width: 5,
                height: _barHeight(index, level),
                margin: const EdgeInsets.symmetric(horizontal: 3),
                decoration: BoxDecoration(
                  color:
                      isSilent
                          ? context.palette.border
                          : context.palette.primary,
                  borderRadius: BorderRadius.circular(AppSizes.pillRadius),
                ),
              ),
          ],
        ),
      ),
    );
  }

  /// 가운데 막대가 가장 크게 반응하도록 중심에서 멀어질수록 진폭을 줄인다.
  double _barHeight(int index, double level) {
    const center = (_barCount - 1) / 2;
    final distance = (index - center).abs() / center;
    final falloff = 1 - (distance * 0.65);
    return _minHeight + (_maxHeight - _minHeight) * level * falloff;
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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const EyebrowLabel('Your sentence · tap to edit'),
        const SizedBox(height: 11),
        // 입력만 카드로 채우고 나머지는 선으로 구분한다. 이 화면에서 손을 대는 곳이
        // 여기 하나뿐이라 유일하게 채워진 면이어야 어디를 눌러야 할지 헷갈리지 않는다.
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
          child: TextField(
            key: const ValueKey('practice-sentence-field'),
            controller: controller,
            focusNode: focusNode,
            // 문장부호·기호가 평가 글자로 전달되지 않도록 입력과 붙여넣기 단계에서 즉시 제거한다.
            inputFormatters: [_customSentenceInputFormatter],
            minLines: 1,
            maxLines: 4,
            textInputAction: TextInputAction.done,
            onSubmitted: (_) => focusNode.unfocus(),
            style: TextStyle(
              color: context.palette.textPrimary,
              fontSize: 21,
              height: 1.45,
              fontWeight: FontWeight.w700,
              letterSpacing: -0.42,
            ),
            decoration: const InputDecoration(
              hintText: 'Type a Korean sentence',
              border: InputBorder.none,
              enabledBorder: InputBorder.none,
              focusedBorder: InputBorder.none,
              contentPadding: EdgeInsets.symmetric(
                horizontal: 12,
                vertical: 13,
              ),
            ),
          ),
        ),
        if (isLoading) ...[
          const SizedBox(height: AppSpacing.md),
          _SentencePreparationStatus(
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
                icon: Icon(
                  Icons.error_outline,
                  size: 18,
                  color: context.palette.error,
                ),
                text: errorText!,
                textColor: context.palette.error,
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
          const SizedBox(height: 26),
          AppCard(
            key: const ValueKey('practice-standard-pronunciation'),
            color: context.palette.blue50,
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const EyebrowLabel('Standard pronunciation'),
                const SizedBox(height: 12),
                Text(
                  preparedSentence!.pronunciation,
                  style: TextStyle(
                    color: context.palette.textPrimary,
                    fontSize: 24,
                    height: 1.45,
                    fontWeight: FontWeight.w900,
                    letterSpacing: -0.6,
                  ),
                ),
                if (preparedSentence!.romanizedPronunciation
                    .trim()
                    .isNotEmpty) ...[
                  const SizedBox(height: 8),
                  RomanizedPronunciation(
                    key: const ValueKey('practice-romanized-pronunciation'),
                    text: preparedSentence!.romanizedPronunciation,
                  ),
                ],
              ],
            ),
          ),
        ],
      ],
    );
  }
}

class _SentencePreparationStatus extends StatelessWidget {
  const _SentencePreparationStatus({
    required this.icon,
    required this.text,
    this.textColor,
  });

  final Widget icon;
  final String text;

  /// 지정하지 않으면 현재 테마의 보조 글자색을 쓴다.
  final Color? textColor;

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
