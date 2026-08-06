// 파일 의도: 추천 문장과 기록 문장을 동일한 정보 계층으로 표시한다.

import 'package:flutter/material.dart';

import '../app/app_palette.dart';
import '../app/app_theme.dart';
import '../models/practice_sentence.dart';
import 'shared_widgets.dart';

/// 문장 목록의 한 행이다.
///
/// 세로로 [한국어 → 로마자 → 영어 번역] 순으로 쌓는다. 대상 사용자가 한글을 읽지 못하므로
/// 한국어만으로는 무엇을 고르는지 알 수 없고, 로마자와 번역이 함께 있어야 선택이 가능하다.
/// 재생 버튼은 두지 않는다. 소리는 문장을 고른 뒤 Practice에서 듣는다.
class SentenceCard extends StatelessWidget {
  const SentenceCard({
    super.key,
    required this.sentence,
    required this.onTap,
    this.actionLabel = 'Practice',
    this.showDivider = false,
    this.isSaved,
    this.onToggleSaved,
    this.highlightRomanization,
  });

  final PracticeSentence sentence;
  final VoidCallback onTap;
  final String actionLabel;
  final bool showDivider;

  /// 저장 상태다. null이면 저장 토글 자체를 두지 않는다(기록 목록 등).
  final bool? isSaved;
  final VoidCallback? onToggleSaved;

  /// 취약 어절의 로마자를 붉게 짚어준다. 어느 부분이 문제인지 목록에서 바로 보이게 한다.
  final String? highlightRomanization;

  @override
  Widget build(BuildContext context) {
    final saved = isSaved;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 14),
          decoration: BoxDecoration(
            border:
                showDivider
                    ? Border(
                      bottom: BorderSide(color: context.palette.lineSubtle),
                    )
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
                      sentence.text,
                      style: TextStyle(
                        color: context.palette.textPrimary,
                        fontSize: 19,
                        height: 1.35,
                        fontWeight: FontWeight.w700,
                        letterSpacing: -0.38,
                      ),
                    ),
                    if (sentence.romanization.isNotEmpty) ...[
                      const SizedBox(height: 5),
                      RomanizationText(
                        sentence.romanization,
                        fontSize: 11.5,
                        highlight: highlightRomanization,
                      ),
                    ],
                    if (sentence.translation.isNotEmpty) ...[
                      const SizedBox(height: 6),
                      Text(
                        sentence.translation,
                        style: TextStyle(
                          color: context.palette.textMuted,
                          fontSize: 12.5,
                          height: 1.4,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              if (saved != null) ...[
                const SizedBox(width: AppSpacing.sm),
                _BookmarkButton(saved: saved, onPressed: onToggleSaved),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

/// 문장 저장 토글이다. 시각 요소는 작아도 히트 영역은 44px를 유지한다.
class _BookmarkButton extends StatelessWidget {
  const _BookmarkButton({required this.saved, required this.onPressed});

  final bool saved;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: saved ? 'Saved. Tap to remove' : 'Save this sentence',
      child: InkWell(
        onTap: onPressed,
        customBorder: const CircleBorder(),
        child: SizedBox(
          width: 44,
          height: 44,
          child: Icon(
            saved ? Icons.bookmark : Icons.bookmark_border,
            size: 20,
            color: saved ? context.palette.primary : context.palette.textMuted,
          ),
        ),
      ),
    );
  }
}
