// 파일 의도: 평가 점수 구간이 사용자에게 동일한 색 의미로 전달되는지 검증한다.
// 선택 이유: 59/60과 79/80 경계는 화면마다 복제하면 서로 다른 색으로 쉽게 어긋난다.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/app/app_theme.dart';
import 'package:lingko_app/widgets/score_card.dart';

void main() {
  testWidgets('score colors use red below 60, orange below 80, blue from 80', (
    tester,
  ) async {
    late BuildContext context;
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Builder(
          builder: (builderContext) {
            context = builderContext;
            return const SizedBox.shrink();
          },
        ),
      ),
    );

    expect(scoreColor(context, 59), const Color(0xFFC0392B));
    expect(scoreColor(context, 60), const Color(0xFF96590C));
    expect(scoreColor(context, 79), const Color(0xFF96590C));
    expect(scoreColor(context, 80), const Color(0xFF245F9B));
    expect(scoreSoftColor(context, 59), const Color(0xFFFDF1EF));
    expect(scoreSoftColor(context, 60), const Color(0xFFFDF5EA));
    expect(scoreSoftColor(context, 80), const Color(0xFFEDF6FD));
    expect(scoreBorderColor(context, 60), const Color(0xFFE3C9A0));
    expect(scoreGaugeColor(context, 80), const Color(0xFF2F73B9));
  });
}
