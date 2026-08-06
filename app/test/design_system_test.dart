// 파일 의도: design-repair 시안에서 확정한 파란 카드형 디자인 토큰의 회귀를 방지한다.
// 기준 문서: docs/design-repair/LingKo Blue Merged.dc.html

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/app/app_palette.dart';
import 'package:lingko_app/app/app_theme.dart';
import 'package:lingko_app/widgets/shared_widgets.dart';

void main() {
  test('design repair color tokens stay aligned with the Flutter theme', () {
    expect(AppColors.primary, const Color(0xFF2F73B9));
    expect(AppColors.primaryDark, const Color(0xFF245F9B));
    expect(AppColors.ctaGradientStart, const Color(0xFF4387CA));
    expect(AppColors.ctaGradientEnd, const Color(0xFF286EAE));
    expect(AppColors.softBlue, const Color(0xFFEDF6FD));
    expect(AppColors.blue50, const Color(0xFFF7FBFF));

    expect(AppColors.scaffold, const Color(0xFFFFFFFF));
    expect(AppColors.card, const Color(0xFFFFFFFF));
    expect(AppColors.textPrimary, const Color(0xFF17324A));
    expect(AppColors.textSecondary, const Color(0xFF5C7386));
    expect(AppColors.textMuted, const Color(0xFF627585));

    expect(AppColors.line, const Color(0xFFDCE7EF));
    expect(AppColors.lineSubtle, const Color(0xFFDCE7EF));
    expect(AppColors.border, const Color(0xFFDCE7EF));
    expect(AppColors.borderStrong, const Color(0xFFA9CAEB));

    // 점수는 80 기준 2단계다. 중간 단계를 추가하면 색 규칙이 깨진다.
    expect(AppColors.success, const Color(0xFF27735A));
    expect(AppColors.error, const Color(0xFFC0392B));
    expect(AppColors.errorSoft, const Color(0xFFFDF1EF));
  });

  test('shape tokens match the blue card and button geometry', () {
    expect(AppSizes.buttonHeight, 52);
    expect(AppSizes.radiusControl, 15);
    expect(AppSizes.radius, 18);
    expect(AppSizes.navigationHeight, 76);
  });

  test('type scale restores the heavy blue design hierarchy', () {
    const allowed = {
      FontWeight.w400,
      FontWeight.w500,
      FontWeight.w700,
      FontWeight.w900,
    };
    final textTheme = AppTheme.light().textTheme;
    final styles = [
      textTheme.headlineLarge,
      textTheme.headlineMedium,
      textTheme.titleLarge,
      textTheme.titleMedium,
      textTheme.bodyLarge,
      textTheme.bodyMedium,
    ];

    for (final style in styles) {
      expect(
        allowed.contains(style!.fontWeight),
        isTrue,
        reason: '허용하지 않은 굵기: ${style.fontWeight}',
      );
    }
  });

  testWidgets('primary buttons and cards restore depth cues', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: Column(
            children: [
              const AppCard(
                key: ValueKey('design-repair-card'),
                child: Text('Card'),
              ),
              PrimaryButton(label: 'Continue', onPressed: () {}),
            ],
          ),
        ),
      ),
    );

    final card = tester.widget<Container>(
      find
          .descendant(
            of: find.byKey(const ValueKey('design-repair-card')),
            matching: find.byType(Container),
          )
          .first,
    );
    final cardDecoration = card.decoration! as BoxDecoration;
    expect(cardDecoration.boxShadow, isNotEmpty);

    final primaryDecoration = tester.widget<DecoratedBox>(
      find.byKey(const ValueKey('primary-button-gradient')),
    );
    final buttonBox = primaryDecoration.decoration as BoxDecoration;
    expect(buttonBox.gradient, isA<LinearGradient>());
    expect(buttonBox.boxShadow, isNotEmpty);
  });

  test('both brightness themes carry a palette', () {
    // 화면이 색을 테마에서 읽으므로 팔레트가 빠지면 밝기 전환이 조용히 무시된다.
    final light = AppTheme.light();
    final dark = AppTheme.dark();

    expect(light.extension<AppPalette>(), isNotNull);
    expect(dark.extension<AppPalette>(), isNotNull);
    expect(light.brightness, Brightness.light);
    expect(dark.brightness, Brightness.dark);
  });

  test('dark palette actually differs from light on every surface', () {
    // 어두운 테마가 밝은 값을 그대로 물려받으면 다크 모드에서 흰 화면이 그대로 남는다.
    const light = AppPalette.light;
    const dark = AppPalette.dark;

    expect(dark.scaffold, isNot(light.scaffold));
    expect(dark.card, isNot(light.card));
    expect(dark.surface, isNot(light.surface));
    expect(dark.textPrimary, isNot(light.textPrimary));
    expect(dark.line, isNot(light.line));
    expect(dark.neutralFill, isNot(light.neutralFill));
    // 강조 배경 위 글자색은 밝기에 따라 반대여야 대비가 유지된다.
    expect(dark.onPrimary, isNot(light.onPrimary));
  });
}
