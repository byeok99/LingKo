import 'dart:async';

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../models/practice_sentence.dart';
import '../services/audio_recorder_service.dart';
import '../widgets/shared_widgets.dart';

class PracticeScreen extends StatefulWidget {
  const PracticeScreen({
    super.key,
    required this.sentence,
    required this.audioRecorderService,
    required this.onEvaluateRecording,
    required this.onCustomSentence,
    required this.onPrepareCustomSentence,
  });

  final PracticeSentence? sentence;
  final AudioRecorderService audioRecorderService;
  final Future<void> Function(PracticeSentence sentence, String audioPath)
  onEvaluateRecording;
  final ValueChanged<PracticeSentence> onCustomSentence;
  final Future<PracticeSentence> Function(String text) onPrepareCustomSentence;

  @override
  State<PracticeScreen> createState() => _PracticeScreenState();
}

class _PracticeScreenState extends State<PracticeScreen> {
  final TextEditingController customSentenceController =
      TextEditingController();
  final FocusNode customSentenceFocusNode = FocusNode();

  bool canSubmitCustomSentence = false;
  bool isPreparingCustomSentence = false;
  String? customSentenceError;
  String? recordedAudioPath;
  bool isRecording = false;
  bool isUploadingRecording = false;
  String? recordingError;
  bool wasPermissionDenied = false;

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
      unawaited(_cleanupRecording(reportErrors: false));
      _syncControllerWithSentence();
      _focusEmptyPracticeInput();
    }
  }

  @override
  void dispose() {
    unawaited(_cleanupRecording(reportErrors: false));
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
    recordedAudioPath = null;
    isRecording = false;
    isUploadingRecording = false;
    recordingError = null;
    wasPermissionDenied = false;
  }

  Future<void> _submitCustomSentence() async {
    final text = customSentenceController.text.trim();

    if (text.isEmpty || isPreparingCustomSentence) {
      return;
    }

    setState(() {
      isPreparingCustomSentence = true;
      customSentenceError = null;
    });

    try {
      final preparedSentence = await widget.onPrepareCustomSentence(text);

      if (!mounted) {
        return;
      }

      widget.onCustomSentence(preparedSentence);
    } catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        customSentenceError = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          isPreparingCustomSentence = false;
        });
      }
    }
  }

  Future<void> _startRecording() async {
    final sentence = widget.sentence;

    if (sentence == null || isRecording || isUploadingRecording) {
      return;
    }

    setState(() {
      recordingError = null;
      recordedAudioPath = null;
    });

    try {
      final hasPermission = await widget.audioRecorderService.hasPermission();
      if (!hasPermission) {
        if (!mounted) {
          return;
        }

        setState(() {
          wasPermissionDenied = true;
          recordingError = 'Microphone permission is required.';
        });
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
    } catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        recordingError = error.toString();
      });
    }
  }

  Future<void> _stopRecording() async {
    if (!isRecording) {
      return;
    }

    try {
      final path = await widget.audioRecorderService.stop();

      if (!mounted) {
        return;
      }

      setState(() {
        isRecording = false;
        recordedAudioPath = path;
        recordingError =
            path == null ? 'Recording did not produce a file.' : null;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        isRecording = false;
        recordingError = error.toString();
      });
    }
  }

  Future<void> _cancelRecording() async {
    await _cleanupRecording(reportErrors: true);
  }

  Future<void> _cleanupRecording({required bool reportErrors}) async {
    final shouldCancel = isRecording;
    final audioPath = recordedAudioPath;

    try {
      if (shouldCancel) {
        await widget.audioRecorderService.cancel();
      }
      if (audioPath != null) {
        await widget.audioRecorderService.delete(audioPath);
      }
    } catch (error) {
      if (!reportErrors || !mounted) {
        return;
      }
      setState(() {
        recordingError = error.toString();
      });
      return;
    }

    if (!mounted || !reportErrors) {
      return;
    }

    setState(() {
      isRecording = false;
      recordedAudioPath = null;
      recordingError = null;
      wasPermissionDenied = false;
    });
  }

  Future<void> _uploadRecording() async {
    final sentence = widget.sentence;
    final audioPath = recordedAudioPath;

    if (sentence == null || audioPath == null || isUploadingRecording) {
      return;
    }

    setState(() {
      isUploadingRecording = true;
      recordingError = null;
    });

    try {
      await widget.onEvaluateRecording(sentence, audioPath);
      await _deleteRecordingBestEffort(audioPath);
      if (mounted) {
        setState(() {
          recordedAudioPath = null;
        });
      }
    } catch (error) {
      if (!mounted) {
        return;
      }

      setState(() {
        recordingError = error.toString();
      });
    } finally {
      if (mounted) {
        setState(() {
          isUploadingRecording = false;
        });
      }
    }
  }

  Future<void> _deleteRecordingBestEffort(String audioPath) async {
    try {
      await widget.audioRecorderService.delete(audioPath);
    } catch (_) {
      // Cleanup must not block a successfully produced evaluation result.
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 18, 20, 24),
      children: [
        const TopBar(title: 'Practice'),
        const SizedBox(height: 22),
        _CustomSentenceCard(
          controller: customSentenceController,
          focusNode: customSentenceFocusNode,
          canSubmit: canSubmitCustomSentence && !isPreparingCustomSentence,
          isLoading: isPreparingCustomSentence,
          errorText: customSentenceError,
          onSubmit: _submitCustomSentence,
        ),
        const SizedBox(height: 24),
        if (widget.sentence == null)
          const _EmptyPracticeState()
        else
          _PracticeContent(
            sentence: widget.sentence!,
            isRecording: isRecording,
            isUploading: isUploadingRecording,
            hasRecording: recordedAudioPath != null,
            errorText: recordingError,
            wasPermissionDenied: wasPermissionDenied,
            onStartRecording: _startRecording,
            onStopRecording: _stopRecording,
            onCancelRecording: _cancelRecording,
            onUploadRecording: _uploadRecording,
          ),
      ],
    );
  }
}

class _PracticeContent extends StatelessWidget {
  const _PracticeContent({
    required this.sentence,
    required this.isRecording,
    required this.isUploading,
    required this.hasRecording,
    required this.errorText,
    required this.wasPermissionDenied,
    required this.onStartRecording,
    required this.onStopRecording,
    required this.onCancelRecording,
    required this.onUploadRecording,
  });

  final PracticeSentence sentence;
  final bool isRecording;
  final bool isUploading;
  final bool hasRecording;
  final String? errorText;
  final bool wasPermissionDenied;
  final VoidCallback onStartRecording;
  final VoidCallback onStopRecording;
  final VoidCallback onCancelRecording;
  final VoidCallback onUploadRecording;

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
        _RecordingPanel(
          isRecording: isRecording,
          isUploading: isUploading,
          hasRecording: hasRecording,
          errorText: errorText,
          wasPermissionDenied: wasPermissionDenied,
          onStartRecording: onStartRecording,
          onStopRecording: onStopRecording,
          onCancelRecording: onCancelRecording,
          onUploadRecording: onUploadRecording,
        ),
      ],
    );
  }
}

class _RecordingPanel extends StatelessWidget {
  const _RecordingPanel({
    required this.isRecording,
    required this.isUploading,
    required this.hasRecording,
    required this.errorText,
    required this.wasPermissionDenied,
    required this.onStartRecording,
    required this.onStopRecording,
    required this.onCancelRecording,
    required this.onUploadRecording,
  });

  final bool isRecording;
  final bool isUploading;
  final bool hasRecording;
  final String? errorText;
  final bool wasPermissionDenied;
  final VoidCallback onStartRecording;
  final VoidCallback onStopRecording;
  final VoidCallback onCancelRecording;
  final VoidCallback onUploadRecording;

  @override
  Widget build(BuildContext context) {
    final primaryLabel =
        isRecording
            ? 'Stop recording'
            : hasRecording
            ? 'Upload and score'
            : wasPermissionDenied
            ? 'Retry microphone permission'
            : 'Start recording';
    final primaryIcon =
        isRecording
            ? Icons.stop
            : hasRecording
            ? Icons.cloud_upload_outlined
            : Icons.mic;
    final primaryAction =
        isUploading
            ? null
            : isRecording
            ? onStopRecording
            : hasRecording
            ? onUploadRecording
            : onStartRecording;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (errorText != null) ...[
          Text(
            errorText!,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: Theme.of(context).colorScheme.error,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 12),
        ],
        FilledButton.icon(
          onPressed: primaryAction,
          icon:
              isUploading
                  ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                  : Icon(primaryIcon),
          label: Text(isUploading ? 'Uploading' : primaryLabel),
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
        if (isRecording || hasRecording) ...[
          const SizedBox(height: 10),
          OutlinedButton.icon(
            onPressed: isUploading ? null : onCancelRecording,
            icon: const Icon(Icons.refresh),
            label: Text(isRecording ? 'Cancel recording' : 'Re-record'),
          ),
        ],
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
          if (errorText != null) ...[
            const SizedBox(height: 10),
            Text(
              errorText!,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: Theme.of(context).colorScheme.error,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: canSubmit ? onSubmit : null,
              icon:
                  isLoading
                      ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                      : const Icon(Icons.arrow_forward),
              label: Text(isLoading ? 'Preparing' : 'Use this sentence'),
            ),
          ),
        ],
      ),
    );
  }
}
