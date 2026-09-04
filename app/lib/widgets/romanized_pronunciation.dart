// 파일 의도: 표준 발음에서 파생된 로마자 읽기 가이드를 화면마다 같은 시각 언어로 표시한다.

import 'package:flutter/material.dart';

import '../app/app_palette.dart';

/// 음절 하이픈과 단어 공백을 보존한 학습자용 로마자 발음 가이드다.
///
/// 서버 값이 없으면 빈 공간도 만들지 않아 이전 API 응답과 안전하게 호환된다.
class RomanizedPronunciation extends StatelessWidget {
  const RomanizedPronunciation({
    super.key,
    required this.text,
    this.textAlign = TextAlign.start,
  });

  final String text;
  final TextAlign textAlign;

  @override
  Widget build(BuildContext context) {
    final guide = text.trim();
    if (guide.isEmpty) {
      return const SizedBox.shrink();
    }

    return Text(
      guide,
      textAlign: textAlign,
      style: TextStyle(
        color: context.palette.textSecondary,
        fontSize: 13,
        fontWeight: FontWeight.w600,
        height: 1.35,
        letterSpacing: 0.15,
      ),
    );
  }
}
