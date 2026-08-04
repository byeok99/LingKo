// 파일 의도: 점수 신뢰 상태 해석이 서버 계약과 어긋날 때 안전한 쪽으로 닫히는지 고정한다.

import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/models/practice_result.dart';
import 'package:lingko_app/models/score_status.dart';

void main() {
  // 서버가 상태값을 추가하거나 응답이 손상돼도 구버전 앱이 점수를 신뢰해버리지 않아야 한다.
  test('unknown wire values fall back to unavailable', () {
    expect(ScoreStatus.fromWire('AVAILABLE'), ScoreStatus.available);
    expect(ScoreStatus.fromWire('UNAVAILABLE'), ScoreStatus.unavailable);

    for (final unknown in <Object?>[
      null,
      '',
      'available',
      'PARTIAL',
      'available ',
      42,
      <String>['AVAILABLE'],
    ]) {
      expect(
        ScoreStatus.fromWire(unknown),
        ScoreStatus.unavailable,
        reason: 'unknown value $unknown must not be treated as trustworthy',
      );
    }
  });

  test('missing scoreStatus is derived from the score field', () {
    expect(ScoreStatus.ofNullableScore(null), ScoreStatus.unavailable);
    expect(ScoreStatus.ofNullableScore(0), ScoreStatus.available);
  });

  // 상태값을 지정하지 않은 생성은 점수를 신뢰하지 않는 쪽이 기본이어야 한다.
  test('word result defaults to unavailable when the status is omitted', () {
    const word = PracticeWordResult(
      position: 0,
      text: '안녕',
      syllables: [],
    );

    expect(word.scoreStatus, ScoreStatus.unavailable);
    expect(word.score, isNull);
  });

  test('word result keeps an unknown server status closed', () {
    final word = PracticeWordResult.fromResultJson(const {
      'position': 0,
      'text': '안녕',
      'score': 91,
      'scoreStatus': 'PARTIAL',
      'syllables': <Object?>[],
    });

    // 점수 숫자가 함께 와도 상태값을 해석할 수 없으면 노출 대상이 아니다.
    expect(word.scoreStatus, ScoreStatus.unavailable);
  });
}
