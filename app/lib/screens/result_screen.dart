// 파일 의도: 종합 점수와 모든 평가 음절을 숨김없이 보여주고 문장 전체 재연습을 연결한다.

import 'dart:async';

import 'package:flutter/material.dart';

import '../app/app_theme.dart';
import '../app/app_palette.dart';
import '../models/practice_result.dart';
import '../models/practice_sentence.dart';
import '../services/sentence_speech_service.dart';
import '../widgets/romanized_pronunciation.dart';
import '../widgets/score_card.dart';
import '../widgets/shared_widgets.dart';
import '../widgets/word_syllable_explorer.dart';

class ResultScreen extends StatelessWidget {
  const ResultScreen({
    super.key,
    required this.sentence,
    required this.result,
    required this.sentenceSpeechService,
    required this.onTryAgain,
    this.isSaved,
    this.onToggleSaved,
    this.onOpenReview,
  });

  final PracticeSentence sentence;
  final PracticeResult? result;
  final SentenceSpeechService sentenceSpeechService;
  final VoidCallback onTryAgain;

  /// 저장 상태다. null이면 저장할 수 없는 문장(직접 입력 등)이라 토글을 두지 않는다.
  final bool? isSaved;
  final VoidCallback? onToggleSaved;
  final VoidCallback? onOpenReview;

  @override
  Widget build(BuildContext context) {
    final currentResult = result;
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 22),
      children: [
        TopBar(
          title: 'Result',
          centered: true,
          // 방금 연습한 문장을 다시 하고 싶을 때가 저장할 마음이 가장 큰 순간이다.
          trailing:
              isSaved == null
                  ? null
                  : IconButton(
                    key: const ValueKey('result-save-sentence'),
                    tooltip: isSaved! ? 'Saved' : 'Save this sentence',
                    onPressed: onToggleSaved,
                    icon: Icon(
                      isSaved! ? Icons.bookmark : Icons.bookmark_border,
                      size: 20,
                      color:
                          isSaved!
                              ? context.palette.primary
                              : context.palette.textMuted,
                    ),
                  ),
        ),
        const SizedBox(height: 10),
        if (currentResult == null)
          StatePanel(
            icon: Icons.info_outline,
            title: 'Result data is unavailable',
            message: 'Return to Practice and evaluate the sentence again.',
            actionLabel: 'Say it again',
            onAction: onTryAgain,
          )
        else ...[
          const SizedBox(height: 6),
          ScoreCard(
            overallScore: currentResult.overallScore,
            accuracy: currentResult.scoreBreakdown.accuracy,
            fluency: currentResult.scoreBreakdown.fluency,
            completeness: currentResult.scoreBreakdown.completeness,
            summary: currentResult.summary,
          ),
          const SizedBox(height: 15),
          // 원문과 표준 발음을 위아래로 붙여 어디가 달라지는지 눈으로 비교하게 한다.
          // 사용자가 실제로 낸 소리를 문자로 재현해 보여주지는 않는다.
          // 보정 없이 정확히 추출할 수 없어 틀린 정보를 사실처럼 보여주게 된다.
          const EyebrowLabel('How it should sound'),
          const SizedBox(height: 12),
          Text(
            sentence.text,
            style: TextStyle(
              color: context.palette.textPrimary,
              fontSize: 21,
              height: 1.5,
              fontWeight: FontWeight.w700,
              letterSpacing: -0.42,
            ),
          ),
          if (sentence.pronunciation.isNotEmpty) ...[
            const SizedBox(height: 5),
            Text(
              sentence.pronunciation,
              style: TextStyle(
                color: context.palette.textSecondary,
                fontSize: 21,
                height: 1.5,
                fontWeight: FontWeight.w500,
                letterSpacing: -0.42,
              ),
            ),
          ],
          if (sentence.romanization.isNotEmpty) ...[
            const SizedBox(height: 7),
            RomanizationText(sentence.romanization, fontSize: 11.5),
          ],
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.only(top: 12),
            decoration: BoxDecoration(
              border: Border(top: BorderSide(color: context.palette.line)),
            ),
            child: const EyebrowLabel('By word · tap to see its syllables'),
          ),
          const SizedBox(height: 6),
          WordSyllableExplorer(words: currentResult.words),
          const SizedBox(height: 20),
          PrimaryButton(
            key: const ValueKey('retry-whole-sentence'),
            label: 'Say it again',
            onPressed: onTryAgain,
          ),
          if (onOpenReview != null) ...[
            const SizedBox(height: AppSpacing.sm),
            TextButton(
              onPressed: onOpenReview,
              child: const Text('View review history'),
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
          if (widget.sentence.romanizedPronunciation.trim().isNotEmpty) ...[
            const SizedBox(height: 5),
            RomanizedPronunciation(
              key: const ValueKey('result-romanized-pronunciation'),
              text: widget.sentence.romanizedPronunciation,
            ),
          ],
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

