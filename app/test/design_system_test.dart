// 파일 의도: preview.html에서 확정한 LingKo 디자인 토큰의 회귀를 방지한다.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
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
}
