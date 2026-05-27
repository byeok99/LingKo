import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:lingko_app/app/lingko_app.dart';

void main() {
  testWidgets('LingKo prototype opens recommended sentences', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const LingKoApp());

    expect(find.text('LingKo'), findsOneWidget);
    expect(find.text('Recommended'), findsOneWidget);
    expect(find.text('맛있겠다.'), findsOneWidget);

    await tester.tap(find.text('맛있겠다.').first);
    await tester.pumpAndSettle();

    expect(find.text('Practice'), findsWidgets);
    final recommendedInput = tester.widget<TextField>(find.byType(TextField));
    expect(recommendedInput.controller?.text, '맛있겠다.');

    await tester.drag(find.byType(Scrollable).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    expect(find.text('Record and score'), findsOneWidget);

    await tester.tap(find.text('Record and score'));
    await tester.pumpAndSettle();

    expect(find.text('Result'), findsOneWidget);
    expect(find.text('Weak sounds'), findsOneWidget);
  });

  testWidgets('Practice tab accepts a custom sentence', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const LingKoApp());

    await tester.tap(find.text('Practice'));
    await tester.pumpAndSettle();

    expect(find.text('Practice your own sentence'), findsOneWidget);
    expect(find.text('맛있겠다.'), findsNothing);
    expect(find.text('Record and score'), findsNothing);

    await tester.enterText(find.byType(TextField), '오늘 날씨가 좋아요.');
    await tester.tap(find.text('Use this sentence'));
    await tester.pumpAndSettle();

    expect(find.text('오늘 날씨가 좋아요.'), findsOneWidget);
  });
}
