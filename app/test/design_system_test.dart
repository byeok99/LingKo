// 파일 의도: Direction A 핸드오프에서 확정한 디자인 토큰의 회귀를 방지한다.
// 기준 문서: docs/design_handoff_lingko_direction_a/README.md
// 이전 기준이던 docs/design/preview.html은 이 리디자인으로 대체됐다.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/app/app_palette.dart';
import 'package:lingko_app/app/app_theme.dart';

void main() {
  test('Direction A color tokens stay aligned with the Flutter theme', () {
    // 인터랙티브 계열은 누를 수 있는 것에만 쓰는 색이라 값이 바뀌면 색 규칙 자체가 흔들린다.
    expect(AppColors.primary, const Color(0xFF2F73B9));
    expect(AppColors.primaryDark, const Color(0xFF245F9B));
    expect(AppColors.softBlue, const Color(0xFFEDF6FD));

    expect(AppColors.scaffold, const Color(0xFFFCFCFB));
    expect(AppColors.card, const Color(0xFFFFFFFF));
    expect(AppColors.textPrimary, const Color(0xFF1B2228));
    expect(AppColors.textSecondary, const Color(0xFF5C7386));
    expect(AppColors.textMuted, const Color(0xFF627585));

    expect(AppColors.line, const Color(0xFFE8E6E1));
    expect(AppColors.lineSubtle, const Color(0xFFEFEDEA));
    expect(AppColors.border, const Color(0xFFE0DED9));
    expect(AppColors.borderStrong, const Color(0xFFD5D3CE));

    // 점수는 80 기준 2단계다. 중간 단계를 추가하면 색 규칙이 깨진다.
    expect(AppColors.success, const Color(0xFF27735A));
    expect(AppColors.error, const Color(0xFFB94A4A));
    expect(AppColors.recordAccent, const Color(0xFFC0453A));
  });

  test('shape tokens match the fixed button and card geometry', () {
    // 버튼 위계를 재질이 아니라 채움으로만 표현하려면 높이와 반경이 고정이어야 한다.
    expect(AppSizes.buttonHeight, 54);
    expect(AppSizes.radiusControl, 12);
    expect(AppSizes.radius, 16);
    expect(AppSizes.navigationHeight, 76);
  });

  test('type scale uses only four weights', () {
    // 이전 테마는 750~950을 섞어 써서 굵기가 위계를 만들지 못했다.
    const allowed = {
      FontWeight.w400,
      FontWeight.w500,
      FontWeight.w600,
      FontWeight.w700,
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

  test('buttons carry no elevation so hierarchy comes from fill only', () {
    final filled = AppTheme.light().filledButtonTheme.style!;

    expect(filled.elevation?.resolve({}), 0);
    // 비활성도 형태를 유지하고 채움만 약화한다.
    expect(
      filled.backgroundColor?.resolve({WidgetState.disabled}),
      AppColors.neutralFill,
    );
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
