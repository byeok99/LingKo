// 파일 의도: app 테마 앱 구성과 전역 표시 정책을 정의한다.
// 선택 이유: 기능 화면이 bootstrap·테마·navigation 세부사항에 의존하지 않도록 app 계층에 둔다.

import 'package:flutter/material.dart';

import 'app_palette.dart';

/// App Colors 앱 전역 구성 책임을 제공한다.
/// 기능별 화면이 전역 테마·최상위 화면 전환 결정을 중복하지 않도록 중앙화했다.
class AppColors {
  const AppColors._();

  static const primaryDark = Color(0xFF174F82);
  static const primaryMedium = Color(0xFF245F9B);
  static const primary = Color(0xFF2F73B9);
  static const primaryLight = Color(0xFF4F91D1);
  static const blue200 = Color(0xFFD6E8F7);
  static const softBlue = Color(0xFFEDF6FD);
  static const blue50 = Color(0xFFF7FBFF);
  static const scaffold = Colors.white;
  static const surface = Color(0xFFF6F9FB);
  static const card = Colors.white;
  static const textStrong = Color(0xFF102B40);
  static const textPrimary = Color(0xFF17324A);
  // 본문 12px에도 쓰이므로 WCAG AA 본문 기준 4.5:1을 넘는 값만 사용한다. 흰 배경 대비 4.94:1.
  static const textSecondary = Color(0xFF5C7386);
  // 하단 탭의 비선택 라벨 색이라 가장 흐린 값이어도 읽을 수 있어야 한다.
  // 흰 배경 4.77:1, 가장 밝은 카드 배경(blue50) 위에서도 4.59:1로 본문 기준을 넘긴다.
  static const textMuted = Color(0xFF627585);
  static const border = Color(0xFFDCE7EF);
  static const success = Color(0xFF27735A);
  static const successSoft = Color(0xFFEAF7F2);
  static const warning = Color(0xFF8A5B16);
  static const warningSoft = Color(0xFFFFF6DF);
  static const error = Color(0xFFB94A4A);
  static const errorSoft = Color(0xFFFFF0F0);
  static const disabled = Color(0xFF8A9AA8);
  static const shadow = Color(0x1317324A);
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
  static const double radiusControl = 14;
  static const double radius = 18;
  static const double radiusLarge = 24;
  static const double pillRadius = 999;
  static const double buttonHeight = 52;
  static const double minimumTouchTarget = 48;
  static const double navigationHeight = 76;
}

/// App 테마 앱 전역 구성 책임을 제공한다.
/// 기능별 화면이 전역 테마·최상위 화면 전환 결정을 중복하지 않도록 중앙화했다.
class AppTheme {
  const AppTheme._();

  static ThemeData light() => _build(AppPalette.light, Brightness.light);

  /// 어두운 환경에서 조용히 연습하는 사용도 흔하므로 같은 구조를 어두운 팔레트로 제공한다.
  static ThemeData dark() => _build(AppPalette.dark, Brightness.dark);

  static ThemeData _build(AppPalette palette, Brightness brightness) {
    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      extensions: [palette],
      scaffoldBackgroundColor: palette.scaffold,
      colorScheme: ColorScheme.fromSeed(
        seedColor: palette.primary,
        brightness: brightness,
        primary: palette.primary,
        onPrimary: palette.onPrimary,
        secondary: palette.primaryMedium,
        surface: palette.card,
        error: palette.error,
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: palette.card,
        foregroundColor: palette.textPrimary,
        centerTitle: true,
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      cardTheme: CardTheme(
        color: palette.card,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSizes.radius),
          side: BorderSide(color: palette.border),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(AppSizes.buttonHeight),
          backgroundColor: palette.primary,
          foregroundColor: palette.onPrimary,
          disabledBackgroundColor: palette.border,
          disabledForegroundColor: palette.textSecondary,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppSizes.radiusControl),
          ),
          textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w900),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size.fromHeight(AppSizes.buttonHeight),
          foregroundColor: palette.primaryDark,
          side: BorderSide(color: palette.primaryLight),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppSizes.radiusControl),
          ),
          textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w900),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: palette.card,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSizes.radiusControl),
          borderSide: BorderSide(color: palette.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSizes.radiusControl),
          borderSide: BorderSide(color: palette.border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSizes.radiusControl),
          borderSide: BorderSide(color: palette.primary, width: 2),
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: palette.card,
        indicatorColor: palette.softBlue,
        height: AppSizes.navigationHeight,
        elevation: 0,
        shadowColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        iconTheme: WidgetStateProperty.resolveWith(
          (states) => IconThemeData(
            color:
                states.contains(WidgetState.selected)
                    ? palette.primaryDark
                    : palette.textMuted,
            size: 21,
          ),
        ),
        labelTextStyle: WidgetStateProperty.resolveWith(
          (states) => TextStyle(
            color:
                states.contains(WidgetState.selected)
                    ? palette.primaryDark
                    : palette.textMuted,
            fontSize: 10,
            fontWeight: FontWeight.w800,
          ),
        ),
      ),
      dividerTheme: DividerThemeData(
        color: palette.border,
        space: 1,
        thickness: 1,
      ),
      textTheme: TextTheme(
        headlineLarge: TextStyle(
          color: palette.textPrimary,
          fontSize: 25,
          fontWeight: FontWeight.w900,
          height: 1.12,
          letterSpacing: -0.6,
        ),
        headlineMedium: TextStyle(
          color: palette.textPrimary,
          fontSize: 20,
          fontWeight: FontWeight.w900,
          height: 1.16,
          letterSpacing: -0.4,
        ),
        titleLarge: TextStyle(
          color: palette.textPrimary,
          fontSize: 16,
          fontWeight: FontWeight.w900,
          letterSpacing: -0.2,
        ),
        titleMedium: TextStyle(
          color: palette.textPrimary,
          fontSize: 14,
          fontWeight: FontWeight.w800,
        ),
        bodyLarge: TextStyle(
          color: palette.textPrimary,
          fontSize: 14,
          height: 1.42,
        ),
        bodyMedium: TextStyle(
          color: palette.textSecondary,
          fontSize: 12,
          height: 1.35,
        ),
      ),
    );
  }
}
