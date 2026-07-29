// 파일 의도: app 테마 앱 구성과 전역 표시 정책을 정의한다.
// 선택 이유: 기능 화면이 bootstrap·테마·navigation 세부사항에 의존하지 않도록 app 계층에 둔다.

import 'package:flutter/material.dart';

/// App Colors 앱 전역 구성 책임을 제공한다.
/// 기능별 화면이 전역 테마·최상위 화면 전환 결정을 중복하지 않도록 중앙화했다.
class AppColors {
  const AppColors._();

  static const primary = Color(0xFF3457D5);
  static const primaryDark = Color(0xFF243EAD);
  static const softBlue = Color(0xFFE9EDFF);
  static const scaffold = Color(0xFFF6F8FC);
  static const card = Colors.white;
  static const textPrimary = Color(0xFF172033);
  static const textSecondary = Color(0xFF566176);
  static const border = Color(0xFFD5DDEA);
  static const success = Color(0xFF18794E);
  static const warning = Color(0xFFA14C22);
  static const error = Color(0xFFB42318);
  static const disabled = Color(0xFFA8B3C3);
  static const shadow = Color(0x0A172033);
  static const providerButtonForeground = Color(0xFF1F1F1F);
  static const providerButtonDisabled = Color(0xFF5F6368);
  static const providerButtonBorder = Color(0xFFDADCE0);
}

/// 화면 간 여백과 터치 크기를 동일하게 유지하는 레이아웃 토큰이다.
class AppSpacing {
  const AppSpacing._();

  static const double xs = 4;
  static const double sm = 8;
  static const double md = 12;
  static const double lg = 16;
  static const double xl = 20;
  static const double xxl = 24;
  static const double section = 32;
}

/// 카드와 버튼의 형태를 일관되게 유지하는 크기 토큰이다.
class AppSizes {
  const AppSizes._();

  static const double radiusSmall = 10;
  static const double radius = 16;
  static const double radiusLarge = 24;
  static const double pillRadius = 999;
  static const double buttonHeight = 54;
  static const double minimumTouchTarget = 48;
}

/// App 테마 앱 전역 구성 책임을 제공한다.
/// 기능별 화면이 전역 테마·최상위 화면 전환 결정을 중복하지 않도록 중앙화했다.
class AppTheme {
  const AppTheme._();

  static ThemeData light() {
    return ThemeData(
      useMaterial3: true,
      scaffoldBackgroundColor: AppColors.scaffold,
      colorScheme: ColorScheme.fromSeed(
        seedColor: AppColors.primary,
        brightness: Brightness.light,
        primary: AppColors.primary,
        secondary: AppColors.primaryDark,
        surface: AppColors.card,
        error: AppColors.error,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.scaffold,
        foregroundColor: AppColors.textPrimary,
        centerTitle: true,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      cardTheme: CardTheme(
        color: AppColors.card,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSizes.radius),
          side: const BorderSide(color: AppColors.border),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(AppSizes.buttonHeight),
          backgroundColor: AppColors.primary,
          foregroundColor: Colors.white,
          disabledBackgroundColor: AppColors.border,
          disabledForegroundColor: AppColors.textSecondary,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
          ),
          textStyle: const TextStyle(fontWeight: FontWeight.w800),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size.fromHeight(AppSizes.buttonHeight),
          foregroundColor: AppColors.primaryDark,
          side: const BorderSide(color: AppColors.border),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
          ),
          textStyle: const TextStyle(fontWeight: FontWeight.w800),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.card,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
          borderSide: const BorderSide(color: AppColors.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
          borderSide: const BorderSide(color: AppColors.border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSizes.radiusSmall),
          borderSide: const BorderSide(color: AppColors.primary, width: 2),
        ),
      ),
      navigationBarTheme: const NavigationBarThemeData(
        backgroundColor: AppColors.card,
        indicatorColor: AppColors.softBlue,
        height: 68,
        labelTextStyle: WidgetStatePropertyAll(
          TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
        ),
      ),
      textTheme: const TextTheme(
        headlineLarge: TextStyle(
          color: AppColors.textPrimary,
          fontSize: 28,
          fontWeight: FontWeight.w800,
          height: 1.12,
        ),
        headlineMedium: TextStyle(
          color: AppColors.textPrimary,
          fontSize: 22,
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
