// 파일 의도: app 테마 앱 구성과 전역 표시 정책을 정의한다.
// 선택 이유: 기능 화면이 bootstrap·테마·navigation 세부사항에 의존하지 않도록 app 계층에 둔다.

import 'package:flutter/material.dart';

import 'app_palette.dart';

/// App Colors 앱 전역 구성 책임을 제공한다.
/// 기능별 화면이 전역 테마·최상위 화면 전환 결정을 중복하지 않도록 중앙화했다.
/// `docs/design-repair` 핸드오프에서 확정한 파란 카드형 밝은 테마 색이다.
///
/// 화면은 이 상수를 직접 참조하지 않고 `AppPalette`를 통해 읽는다. 여기 값은 밝은 테마의
/// 정의이자 `docs/design-repair`와 대조하는 기준점이다.
class AppColors {
  const AppColors._();

  // 인터랙티브 — 누를 수 있는 것에만 쓴다. 비인터랙티브 텍스트에 쓰지 않는다.
  static const primary = Color(0xFF2F73B9);
  static const primaryDark = Color(0xFF245F9B);
  static const primaryMedium = Color(0xFF245F9B);
  static const primaryLight = Color(0xFF4F91D1);
  static const ctaGradientStart = Color(0xFF4387CA);
  static const ctaGradientEnd = Color(0xFF286EAE);
  static const softBlue = Color(0xFFEDF6FD);
  static const blue200 = Color(0xFFD6E8F7);
  static const blue50 = Color(0xFFF7FBFF);

  // 면
  static const scaffold = Color(0xFFFFFFFF);
  static const card = Color(0xFFFFFFFF);
  static const surface = Color(0xFFF7FBFF);

  // 글자
  static const textStrong = Color(0xFF17324A);
  static const textPrimary = Color(0xFF17324A);
  // 본문 12~13px에도 쓰이므로 WCAG AA 본문 기준을 넘는 값만 쓴다. 배경 대비 4.81:1.
  static const textSecondary = Color(0xFF5C7386);
  // 비활성 탭 라벨·메타 정보. 배경 대비 4.65:1.
  static const textMuted = Color(0xFF627585);

  // 선 — 카드 테두리와 리스트 구분선을 구분해 위계를 만든다.
  static const line = Color(0xFFDCE7EF);
  static const lineSubtle = Color(0xFFDCE7EF);
  static const border = Color(0xFFDCE7EF);
  static const borderStrong = Color(0xFFA9CAEB);

  // 점수 — 80 기준 2단계다. 중간 단계는 두지 않는다.
  static const success = Color(0xFF27735A);
  static const successSoft = Color(0xFFEEF4F1);
  static const error = Color(0xFFC0392B);
  static const errorSoft = Color(0xFFFDF1EF);
  static const errorBorder = Color(0xFFE8B8B3);

  // 상태·기타
  static const warning = Color(0xFF8A5B16);
  static const warningSoft = Color(0xFFFFF6DF);
  static const neutralFill = Color(0xFFEEF3F7);
  static const recordAccent = Color(0xFFC0453A);

  /// 비활성 버튼 글자다. 핸드오프의 #627585는 채움 위에서 4.19:1로 본문 기준에 못 미쳐
  /// 기준을 넘는 가장 가까운 값으로 낮췄다. 에너지 소진을 알리는 버튼이라 읽혀야 한다.
  static const disabled = Color(0xFF566E82);
  static const shadow = Color(0x1317324A);

  // 제공자 브랜드 규정을 따르는 예외라 테마와 무관하게 고정한다.
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

  static const double radiusSmall = 11;

  /// 버튼 반경. 파란 CTA가 카드와 같은 부드러운 곡률을 갖게 한다.
  static const double radiusControl = 15;

  /// 카드 반경.
  static const double radius = 18;

  /// 한 덩어리로 읽히는 작은 타일 반경. 취약 음절 타일, Review 점수 배지가 쓴다.
  static const double radiusTile = 14;

  /// bottom sheet 상단 반경.
  static const double radiusLarge = 22;
  static const double pillRadius = 999;

  /// 모든 버튼 높이를 고정해 위계를 크기가 아니라 채움으로만 표현한다.
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
          // 공통 PrimaryButton이 그라디언트를 그리므로 Material 자체는 투명하게 쓴다.
          elevation: 0,
          disabledBackgroundColor: palette.neutralFill,
          disabledForegroundColor: palette.disabled,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppSizes.radiusControl),
          ),
          textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w900),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size.fromHeight(AppSizes.buttonHeight),
          foregroundColor: palette.textPrimary,
          // secondary는 동등한 선택지이므로 강조색이 아니라 중립 테두리를 쓴다.
          side: BorderSide(color: palette.borderStrong),
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
            fontSize: 10.5,
            fontWeight:
                states.contains(WidgetState.selected)
                    ? FontWeight.w900
                    : FontWeight.w700,
          ),
        ),
      ),
      dividerTheme: DividerThemeData(
        color: palette.lineSubtle,
        space: 1,
        thickness: 1,
      ),
      textTheme: TextTheme(
        headlineLarge: TextStyle(
          color: palette.textPrimary,
          fontSize: 28,
          fontWeight: FontWeight.w900,
          height: 1.3,
          letterSpacing: -0.9,
        ),
        headlineMedium: TextStyle(
          color: palette.textPrimary,
          fontSize: 20,
          fontWeight: FontWeight.w900,
          height: 1.2,
          letterSpacing: -0.66,
        ),
        titleLarge: TextStyle(
          color: palette.textPrimary,
          fontSize: 16,
          fontWeight: FontWeight.w900,
          letterSpacing: -0.43,
        ),
        titleMedium: TextStyle(
          color: palette.textPrimary,
          fontSize: 14,
          fontWeight: FontWeight.w900,
        ),
        bodyLarge: TextStyle(
          color: palette.textPrimary,
          fontSize: 13,
          fontWeight: FontWeight.w400,
          height: 1.6,
        ),
        bodyMedium: TextStyle(
          color: palette.textSecondary,
          fontSize: 12.5,
          fontWeight: FontWeight.w400,
          height: 1.55,
        ),
      ),
    );
  }
}
