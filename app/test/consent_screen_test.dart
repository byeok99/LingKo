// 파일 의도: 회원가입 동의 화면이 지켜야 하는 계약을 회귀 테스트로 고정한다.
// 보장 대상: 필수 항목 미충족 시 진행 차단, 선택 항목이 진행을 막지 않을 것,
// 전체 동의의 범위, 상위로 올라가는 ConsentSelection 값의 정확성.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/app/app_theme.dart';
import 'package:lingko_app/models/consent_selection.dart';
import 'package:lingko_app/screens/consent_screen.dart';

void main() {
  /// 화면을 테마와 함께 띄운다. 팔레트 확장을 쓰므로 앱 테마가 필요하다.
  Future<void> pumpConsent(
    WidgetTester tester, {
    void Function(ConsentSelection)? onAgree,
    void Function(ConsentDocument)? onOpenDocument,
    VoidCallback? onCancel,
  }) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: ConsentScreen(
          onAgree: onAgree ?? (_) {},
          onOpenDocument: onOpenDocument ?? (_) {},
          onCancel: onCancel ?? () {},
        ),
      ),
    );
  }

  /// 계속하기 버튼이 눌리는 상태인지 확인한다.
  ///
  /// PrimaryButton은 내부에서 FilledButton을 그린다. find.byType은 정확한 런타임 타입만
  /// 맞추므로 상위 추상 타입(ButtonStyleButton)이 아니라 FilledButton으로 찾아야 한다.
  bool isContinueEnabled(WidgetTester tester) {
    final button = tester.widget<FilledButton>(
      find.descendant(
        of: find.byKey(const Key('consent-continue')),
        matching: find.byType(FilledButton),
      ),
    );
    return button.onPressed != null;
  }

  testWidgets('필수 항목을 채우기 전에는 가입을 진행할 수 없다', (tester) async {
    await pumpConsent(tester);

    expect(isContinueEnabled(tester), isFalse);

    await tester.tap(find.byKey(const Key('consent-terms')));
    await tester.pump();
    // 필수 두 개 중 하나만 채운 상태에서도 여전히 막혀야 한다.
    expect(isContinueEnabled(tester), isFalse);

    await tester.tap(find.byKey(const Key('consent-privacy')));
    await tester.pump();
    expect(isContinueEnabled(tester), isTrue);
  });

  testWidgets('선택 항목을 거부해도 가입을 진행할 수 있다', (tester) async {
    ConsentSelection? captured;
    await pumpConsent(tester, onAgree: (selection) => captured = selection);

    await tester.tap(find.byKey(const Key('consent-terms')));
    await tester.tap(find.byKey(const Key('consent-privacy')));
    await tester.pump();

    await tester.tap(find.byKey(const Key('consent-continue')));
    await tester.pump();

    expect(captured, isNotNull);
    expect(captured!.termsAgreed, isTrue);
    expect(captured!.privacyAcknowledged, isTrue);
    // 마케팅은 켜지 않았으므로 거부로 올라가야 한다.
    expect(captured!.marketingOptIn, isFalse);
    expect(captured!.canProceed, isTrue);
  });

  testWidgets('선택 항목만으로는 가입을 진행할 수 없다', (tester) async {
    await pumpConsent(tester);

    await tester.tap(find.byKey(const Key('consent-marketing')));
    await tester.pump();

    expect(isContinueEnabled(tester), isFalse);
  });

  testWidgets('전체 동의는 선택 항목까지 함께 켜고 끈다', (tester) async {
    ConsentSelection? captured;
    await pumpConsent(tester, onAgree: (selection) => captured = selection);

    await tester.tap(find.byKey(const Key('consent-agree-all')));
    await tester.pump();
    expect(isContinueEnabled(tester), isTrue);

    await tester.tap(find.byKey(const Key('consent-continue')));
    await tester.pump();
    expect(captured!.marketingOptIn, isTrue);
  });

  testWidgets('전체 동의를 다시 누르면 모든 항목이 해제된다', (tester) async {
    await pumpConsent(tester);

    await tester.tap(find.byKey(const Key('consent-agree-all')));
    await tester.pump();
    await tester.tap(find.byKey(const Key('consent-agree-all')));
    await tester.pump();

    expect(isContinueEnabled(tester), isFalse);
  });

  testWidgets('필수 항목에서만 전문 보기를 제공하고 해당 문서를 상위로 알린다', (tester) async {
    final opened = <ConsentDocument>[];
    await pumpConsent(tester, onOpenDocument: opened.add);

    // 마케팅 항목에는 열어볼 문서가 없으므로 보기 버튼을 두지 않는다.
    expect(find.widgetWithText(TextButton, 'View'), findsNWidgets(2));

    await tester.tap(find.byKey(const Key('consent-terms-view')));
    await tester.pump();
    await tester.tap(find.byKey(const Key('consent-privacy-view')));
    await tester.pump();

    expect(opened, [
      ConsentDocument.termsOfService,
      ConsentDocument.privacyPolicy,
    ]);
  });

  testWidgets('선택 항목 거부에 불이익이 없다는 안내와 연령 안내를 함께 보여준다', (tester) async {
    await pumpConsent(tester);

    expect(find.byKey(const Key('consent-optional-notice')), findsOneWidget);
    expect(find.byKey(const Key('consent-age-notice')), findsOneWidget);
  });

  testWidgets('뒤로 가기는 동의 없이 이전 화면으로 돌아간다', (tester) async {
    var cancelled = false;
    await pumpConsent(tester, onCancel: () => cancelled = true);

    await tester.tap(find.byKey(const Key('consent-back')));
    await tester.pump();

    expect(cancelled, isTrue);
  });

  test('동의 값은 문서 버전과 동의 시각을 함께 담는다', () {
    final selection = ConsentSelection(
      termsAgreed: true,
      privacyAcknowledged: true,
      marketingOptIn: false,
      documentVersion: consentDocumentVersion,
      agreedAt: DateTime.utc(2026, 8, 7, 1, 2, 3),
    );

    expect(selection.toJson(), {
      'termsAgreed': true,
      'privacyAcknowledged': true,
      'marketingOptIn': false,
      'documentVersion': '2026-08-07',
      'agreedAt': '2026-08-07T01:02:03.000Z',
    });
  });
}
