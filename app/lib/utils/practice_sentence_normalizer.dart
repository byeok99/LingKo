// 파일 의도: 연습 문장의 특수기호 제거 규칙을 입력·API·상태 경계에서 공유한다.

import 'dart:math' as math;

import 'package:flutter/services.dart';

final RegExp _unsupportedPracticeSentencePattern = RegExp(
  r'[\p{P}\p{S}]',
  unicode: true,
);
final RegExp _practiceSentenceWhitespacePattern = RegExp(r'\s+');

/// 문장부호와 기호를 제거하고 공백을 하나로 정리한 평가용 문장을 반환한다.
String normalizePracticeSentenceText(String value) {
  return value
      .replaceAll(_unsupportedPracticeSentencePattern, '')
      .replaceAll(_practiceSentenceWhitespacePattern, ' ')
      .trim();
}

/// 한국어 IME 조합은 유지하면서 새로 입력된 문장부호와 기호만 즉시 제거한다.
class PracticeSentenceInputFormatter extends TextInputFormatter {
  const PracticeSentenceInputFormatter();

  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final filteredText = newValue.text.replaceAll(
      _unsupportedPracticeSentencePattern,
      '',
    );
    if (filteredText == newValue.text) {
      return newValue;
    }

    int filteredOffset(int offset) {
      final safeOffset = math.min(math.max(offset, 0), newValue.text.length);
      return newValue.text
          .substring(0, safeOffset)
          .replaceAll(_unsupportedPracticeSentencePattern, '')
          .length;
    }

    return TextEditingValue(
      text: filteredText,
      selection: TextSelection(
        baseOffset: filteredOffset(newValue.selection.baseOffset),
        extentOffset: filteredOffset(newValue.selection.extentOffset),
        affinity: newValue.selection.affinity,
        isDirectional: newValue.selection.isDirectional,
      ),
      // 허용되지 않은 문자가 조합 범위에 포함되면 기존 범위가 더 이상 유효하지 않다.
      composing: TextRange.empty,
    );
  }
}
