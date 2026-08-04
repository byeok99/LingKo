// 파일 의도: 종합 점수와 모든 평가 음절을 숨김없이 보여주고 문장 전체 재연습을 연결한다.

import 'dart:async';

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import '../models/practice_result.dart';
import '../models/practice_sentence.dart';
import '../services/sentence_speech_service.dart';
import '../widgets/score_breakdown.dart';
import '../widgets/shared_widgets.dart';
import '../widgets/word_syllable_explorer.dart';

class ResultScreen extends StatelessWidget {
  const ResultScreen({
    super.key,
    required this.sentence,
    required this.result,
    required this.sentenceSpeechService,
    required this.onTryAgain,
    this.onOpenReview,
  });

  final PracticeSentence sentence;
  final PracticeResult? result;
  final SentenceSpeechService sentenceSpeechService;
  final VoidCallback onTryAgain;
  final VoidCallback? onOpenReview;

  @override
  Widget build(BuildContext context) {
    final currentResult = result;
    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 15, 18, 22),
      children: [
        const TopBar(title: 'Result', centered: true),
        const SizedBox(height: 10),
        if (currentResult == null)
          StatePanel(
            icon: Icons.info_outline,
            title: 'Result data is unavailable',
            message: 'Return to Practice and evaluate the sentence again.',
            actionLabel: 'Try This Sentence Again',
            onAction: onTryAgain,
          )
        else ...[
          AppCard(
            padding: const EdgeInsets.all(15),
            child: Row(
              children: [
                ScoreRing(score: currentResult.overallScore),
                const SizedBox(width: 15),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      StatusBadge(
                        label: currentResult.gradeLabel,
                        tone: _gradeTone(currentResult.overallScore),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        currentResult.summary,
                        style: Theme.of(context).textTheme.bodyMedium,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          SectionHeader(title: 'Pronunciation guide'),
          const SizedBox(height: 10),
          _PronunciationGuideCard(
            sentence: sentence,
            sentenceSpeechService: sentenceSpeechService,
          ),
          const SizedBox(height: 24),
          SectionHeader(title: 'Score breakdown'),
          const SizedBox(height: 10),
          ScoreBreakdown(
            accuracy: currentResult.scoreBreakdown.accuracy,
            fluency: currentResult.scoreBreakdown.fluency,
            completeness: currentResult.scoreBreakdown.completeness,
          ),
          const SizedBox(height: 24),
          SectionHeader(title: 'Pronunciation by word'),
          const SizedBox(height: 10),
          WordSyllableExplorer(words: currentResult.words),
          if (currentResult.weakCharacters.isNotEmpty) ...[
            const SizedBox(height: 24),
            SectionHeader(title: 'Detailed feedback'),
            const SizedBox(height: 10),
            for (
              var index = 0;
              index < currentResult.weakCharacters.length;
              index++
            ) ...[
              _FeedbackRow(result: currentResult.weakCharacters[index]),
              if (index != currentResult.weakCharacters.length - 1)
                const SizedBox(height: 8),
            ],
          ],
          const SizedBox(height: 24),
          PrimaryButton(
            key: const ValueKey('retry-whole-sentence'),
            label: 'Try This Sentence Again',
            icon: Icons.refresh,
            onPressed: onTryAgain,
          ),
          if (onOpenReview != null) ...[
            const SizedBox(height: AppSpacing.sm),
            SecondaryButton(
              label: 'Open Review',
              icon: Icons.history,
              onPressed: onOpenReview,
            ),
          ],
        ],
      ],
    );
  }
}

class _PronunciationGuideCard extends StatefulWidget {
  const _PronunciationGuideCard({
    required this.sentence,
    required this.sentenceSpeechService,
  });

  final PracticeSentence sentence;
  final SentenceSpeechService sentenceSpeechService;

  @override
  State<_PronunciationGuideCard> createState() =>
      _PronunciationGuideCardState();
}

class _PronunciationGuideCardState extends State<_PronunciationGuideCard> {
  String? speechError;

  String get standardPronunciation {
    final pronunciation = widget.sentence.pronunciation.trim();
    return pronunciation.isEmpty ? widget.sentence.text : pronunciation;
  }

  @override
  void dispose() {
    unawaited(widget.sentenceSpeechService.stop());
    super.dispose();
  }

  Future<void> _speak(SentenceSpeechRate rate) async {
    setState(() => speechError = null);
    try {
      // 평가 후에는 화면의 표준 발음 표기와 실제 듣기 대상을 일치시킨다.
      await widget.sentenceSpeechService.speak(
        standardPronunciation,
        rate: rate,
      );
    } catch (_) {
      if (mounted) {
        setState(() {
          speechError =
              'The standard pronunciation could not be played. Check the device volume and Korean voice settings.';
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return AppCard(
      color: context.palette.blue50,
      padding: const EdgeInsets.all(15),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'Sentence',
            style: TextStyle(
              color: context.palette.textSecondary,
              fontSize: 11,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 5),
          Text(
            widget.sentence.text,
            style: TextStyle(
              color: context.palette.textPrimary,
              fontSize: 16,
              fontWeight: FontWeight.w800,
            ),
          ),
          Padding(
            padding: EdgeInsets.symmetric(vertical: 13),
            child: Divider(height: 1, color: context.palette.border),
          ),
          Row(
            children: [
              Icon(
                Icons.record_voice_over_outlined,
                size: 17,
                color: context.palette.primary,
              ),
              SizedBox(width: 6),
              Text(
                'Standard pronunciation',
                style: TextStyle(
                  color: context.palette.primaryDark,
                  fontSize: 12,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ],
          ),
          const SizedBox(height: 7),
          Text(
            standardPronunciation,
            key: const ValueKey('result-standard-pronunciation'),
            style: TextStyle(
              color: context.palette.primaryDark,
              fontSize: 24,
              fontWeight: FontWeight.w900,
              height: 1.35,
            ),
          ),
          const SizedBox(height: 13),
          Row(
            children: [
              Expanded(
                child: ActionButton(
                  key: const ValueKey('result-play-pronunciation-normal'),
                  icon: Icons.play_arrow,
                  label: 'Normal',
                  onPressed: () => _speak(SentenceSpeechRate.normal),
                ),
              ),
              const SizedBox(width: 9),
              Expanded(
                child: ActionButton(
                  key: const ValueKey('result-play-pronunciation-slow'),
                  icon: Icons.slow_motion_video,
                  label: 'Slow',
                  onPressed: () => _speak(SentenceSpeechRate.slow),
                ),
              ),
            ],
          ),
          if (speechError != null) ...[
            const SizedBox(height: 10),
            Text(
              speechError!,
              style: TextStyle(
                color: context.palette.error,
                fontSize: 12,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _FeedbackRow extends StatelessWidget {
  const _FeedbackRow({required this.result});

  final CharacterResult result;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      padding: const EdgeInsets.all(11),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: context.palette.errorSoft,
              borderRadius: BorderRadius.circular(13),
            ),
            child: Text(
              result.character,
              style: TextStyle(
                color: context.palette.error,
                fontSize: 18,
                fontWeight: FontWeight.w900,
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  result.note.isEmpty
                      ? 'Keep practicing this sound'
                      : result.note,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 2),
                Text(
                  result.scoreStatus.isAvailable
                      ? 'Score ${result.score}'
                      : 'Score unavailable',
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

StatusTone _gradeTone(int score) {
  if (score >= 90) {
    return StatusTone.success;
  }
  if (score >= 75) {
    return StatusTone.info;
  }
  if (score >= 60) {
    return StatusTone.warning;
  }
  return StatusTone.error;
}
