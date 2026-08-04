// 파일 의도: preview.html에서 확정한 LingKo 디자인 토큰의 회귀를 방지한다.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lingko_app/app/app_palette.dart';
import 'package:lingko_app/app/app_theme.dart';

void main() {
  test('preview design tokens stay aligned with the Flutter theme', () {
    expect(AppColors.primary, const Color(0xFF2F73B9));
    expect(AppColors.primaryDark, const Color(0xFF174F82));
    expect(AppColors.softBlue, const Color(0xFFEDF6FD));
    expect(AppColors.scaffold, Colors.white);
    expect(AppColors.textPrimary, const Color(0xFF17324A));
    // 본문 크기에서도 WCAG AA(4.5:1)를 넘도록 preview.html과 함께 낮춘 값이다.
    expect(AppColors.textSecondary, const Color(0xFF5C7386));
    expect(AppColors.textMuted, const Color(0xFF627585));
    expect(AppColors.border, const Color(0xFFDCE7EF));
    expect(AppSizes.radius, 18);
    expect(AppSizes.buttonHeight, 52);
    expect(AppSizes.navigationHeight, 76);
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
    expect(dark.border, isNot(light.border));
    // 강조 배경 위 글자색은 밝기에 따라 반대여야 대비가 유지된다.
    expect(dark.onPrimary, isNot(light.onPrimary));
  });
}
