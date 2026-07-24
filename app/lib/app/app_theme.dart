// 파일 의도: app 테마 앱 구성과 전역 표시 정책을 정의한다.
// 선택 이유: 기능 화면이 bootstrap·테마·navigation 세부사항에 의존하지 않도록 app 계층에 둔다.

import 'package:flutter/material.dart';

/// App Colors 앱 전역 구성 책임을 제공한다.
/// 기능별 화면이 전역 테마·최상위 화면 전환 결정을 중복하지 않도록 중앙화했다.
class AppColors {
  const AppColors._();

  static const background = Colors.white;
  static const surface = Color(0xFFF7FBFF);
  static const brand = Color(0xFFBBD7F2);
  static const brandStrong = Color(0xFF8FB9E1);
  static const brandSoft = Color(0xFFEAF4FC);
  static const textPrimary = Color(0xFF15324A);
  static const textSecondary = Color(0xFF5C7286);
  static const border = Color(0xFFD9E7F3);
  static const info = Color(0xFF2F6E9F);
  static const success = Color(0xFF2E7D61);
}

/// App 테마 앱 전역 구성 책임을 제공한다.
/// 기능별 화면이 전역 테마·최상위 화면 전환 결정을 중복하지 않도록 중앙화했다.
class AppTheme {
  const AppTheme._();

  static ThemeData light() {
    return ThemeData(
      useMaterial3: true,
      scaffoldBackgroundColor: AppColors.background,
      colorScheme: ColorScheme.fromSeed(
        seedColor: AppColors.brand,
        brightness: Brightness.light,
        primary: AppColors.brand,
        surface: AppColors.background,
      ),
      textTheme: const TextTheme(
        headlineLarge: TextStyle(
          color: AppColors.textPrimary,
          fontSize: 30,
          fontWeight: FontWeight.w800,
          height: 1.12,
        ),
        headlineMedium: TextStyle(
          color: AppColors.textPrimary,
          fontSize: 24,
          fontWeight: FontWeight.w800,
          height: 1.16,
        ),
        titleLarge: TextStyle(
          color: AppColors.textPrimary,
          fontSize: 18,
          fontWeight: FontWeight.w800,
        ),
        titleMedium: TextStyle(
          color: AppColors.textPrimary,
          fontSize: 16,
          fontWeight: FontWeight.w700,
        ),
        bodyLarge: TextStyle(
          color: AppColors.textPrimary,
          fontSize: 16,
          height: 1.42,
        ),
        bodyMedium: TextStyle(
          color: AppColors.textSecondary,
          fontSize: 14,
          height: 1.35,
        ),
      ),
    );
  }
}
