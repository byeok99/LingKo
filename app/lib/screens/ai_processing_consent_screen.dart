import 'package:flutter/material.dart';
import '../models/ai_processing_consent.dart';

/// 외부 전송 전에 수신자·항목·목적을 고지하고 사용자의 명시적 선택을 받는다.
class AiProcessingConsentScreen extends StatefulWidget {
  const AiProcessingConsentScreen({
    super.key,
    required this.status,
    required this.onSave,
    required this.onOpenPrivacy,
  });
  final AiProcessingConsent status;
  final Future<AiProcessingConsent> Function(bool granted) onSave;
  final VoidCallback onOpenPrivacy;

  @override
  State<AiProcessingConsentScreen> createState() =>
      _AiProcessingConsentScreenState();
}

class _AiProcessingConsentScreenState extends State<AiProcessingConsentScreen> {
  bool saving = false;
  String? error;

  Future<void> save(bool granted) async {
    setState(() {
      saving = true;
      error = null;
    });
    try {
      final result = await widget.onSave(granted);
      if (granted && (!result.granted || !result.isSupported)) {
        throw StateError('Consent was not accepted');
      }
      if (mounted) Navigator.pop(context, granted);
    } catch (_) {
      if (mounted) {
        setState(
          () =>
              error =
                  'Could not save your choice. No new recording will be sent. Please try again.',
        );
      }
    } finally {
      if (mounted) setState(() => saving = false);
    }
  }

  @override
  Widget build(BuildContext context) => PopScope(
    canPop: !saving,
    child: Scaffold(
      appBar: AppBar(title: const Text('AI assessment privacy')),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Text(
            'Before sharing your recording',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 20),
          const Text(
            'To assess your pronunciation, LingKo uploads your voice recording to Amazon Web Services (AWS S3) and sends the recording and the reference pronunciation of your practice sentence to Microsoft Azure AI Speech. Microsoft processes them to return pronunciation scores and recognized speech.',
          ),
          const SizedBox(height: 16),
          const Text(
            'Your recording can contain your voice and anything you say. Your custom sentence may also contain personal information. Avoid including sensitive information. We do not send your account name or email to Azure for this assessment.',
          ),
          const SizedBox(height: 16),
          const Text(
            'LingKo stores your practice text and assessment results in your account. Uploaded audio is deleted after completion or terminal failure; temporary or abandoned uploads are cleaned up according to our Privacy Policy. Azure pronunciation assessment does not retain the submitted audio or reference text, according to Microsoft’s service documentation.',
          ),
          const SizedBox(height: 16),
          const Text(
            'Replicate generates visual guides using LingKo’s guide images, not your voice recording. This permission is for voice assessment, not advertising.',
          ),
          const SizedBox(height: 16),
          const Text(
            'You can decline and keep using sentence browsing and guides. AI assessment requires permission. You can withdraw it in Profile → AI assessment privacy. Withdrawal stops new assessments and queued work at the next consent check; a transmission already started cannot be recalled. Existing results remain until you delete your account.',
          ),
          TextButton(
            onPressed: widget.onOpenPrivacy,
            child: const Text('Read Privacy Policy'),
          ),
          if (!widget.status.isSupported)
            const Text(
              'This privacy notice has changed. Update LingKo before allowing AI assessment.',
            ),
          if (error != null)
            Text(
              error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          const SizedBox(height: 16),
          if (!widget.status.granted && widget.status.isSupported)
            FilledButton(
              onPressed: saving ? null : () => save(true),
              child: Text(
                saving ? 'Saving…' : 'Allow sharing with Microsoft Azure',
              ),
            ),
          if (widget.status.granted)
            OutlinedButton(
              onPressed: saving ? null : () => save(false),
              child: const Text('Withdraw permission'),
            ),
          TextButton(
            onPressed: saving ? null : () => Navigator.pop(context, false),
            child: Text(
              widget.status.granted ? 'Close' : 'Not now — do not send',
            ),
          ),
        ],
      ),
    ),
  );
}
