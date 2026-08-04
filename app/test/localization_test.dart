// 파일 의도: 표시 언어 설정이 실제 UI 언어로 이어지는지와 번역 누락 시 동작을 고정한다.

import 'package:flutter/material.dart';
import 'package:flutter_gen/gen_l10n/app_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/app/lingko_app.dart';

void main() {
  test('supported locales cover the languages offered in settings', () {
    // Profile에서 고를 수 있는 언어인데 지원 목록에 없으면, 사용자가 고른 뒤
    // 아무 변화도 일어나지 않는 상태가 된다. 두 목록은 항상 같이 움직여야 한다.
    final codes =
        AppL10n.supportedLocales.map((locale) => locale.languageCode).toSet();

    expect(codes, containsAll(<String>['en', 'ko', 'ja']));
  });

  testWidgets('locale override drives the rendered language', (
    WidgetTester tester,
  ) async {
    late AppL10n english;
    late AppL10n korean;

    await tester.pumpWidget(
      MaterialApp(
        locale: const Locale('en'),
        localizationsDelegates: AppL10n.localizationsDelegates,
        supportedLocales: AppL10n.supportedLocales,
        home: Builder(
          builder: (context) {
            english = AppL10n.of(context);
            return const SizedBox.shrink();
          },
        ),
      ),
    );

    await tester.pumpWidget(
      MaterialApp(
        locale: const Locale('ko'),
        localizationsDelegates: AppL10n.localizationsDelegates,
        supportedLocales: AppL10n.supportedLocales,
        home: Builder(
          builder: (context) {
            korean = AppL10n.of(context);
            return const SizedBox.shrink();
          },
        ),
      ),
    );

    expect(english.localeName, 'en');
    // 아직 번역이 없는 로케일도 앱이 깨지지 않고 템플릿 문자열로 대체되어야 한다.
    expect(korean.localeName, 'ko');
    expect(korean.home, isNotEmpty);
  });

  testWidgets('LingKoApp accepts a locale override', (
    WidgetTester tester,
  ) async {
    // 설정에서 고른 표시 언어가 MaterialApp까지 전달되는 통로가 유지되어야 한다.
    const app = LingKoApp(localeOverride: Locale('ja'));

    expect(app.localeOverride, const Locale('ja'));
  });
}
