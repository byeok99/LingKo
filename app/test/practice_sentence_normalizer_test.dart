// 파일 의도: 연습 문장 정규화와 입력 formatter의 회귀 계약을 검증한다.

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/utils/practice_sentence_normalizer.dart';

void main() {
  test('normalizer removes punctuation, symbols, and repeated whitespace', () {
    expect(
      normalizePracticeSentenceText(
        '  안녕하세요.!?  @LingKo #1 (연습)_테스트-좋아요😊₩  ',
      ),
      '안녕하세요 LingKo 1 연습테스트좋아요',
    );
  });

  test('formatter removes symbols and keeps the caret at valid text', () {
    const formatter = PracticeSentenceInputFormatter();
    const oldValue = TextEditingValue(
      text: '안녕',
      selection: TextSelection.collapsed(offset: 2),
    );
    const newValue = TextEditingValue(
      text: '안녕.!?',
      selection: TextSelection.collapsed(offset: 5),
    );

    final formatted = formatter.formatEditUpdate(oldValue, newValue);

    expect(formatted.text, '안녕');
    expect(formatted.selection, const TextSelection.collapsed(offset: 2));
  });
}
