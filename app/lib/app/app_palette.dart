// 파일 의도: 밝기별로 달라지는 색을 한 곳에서 정의하고 화면이 현재 테마의 값을 읽게 한다.
// 선택 이유: 정적 상수는 다크 모드에서 바뀔 수 없어, 테마에 실려 다니는 확장으로 옮겼다.

import 'package:flutter/material.dart';

import 'app_theme.dart';

/// 화면이 사용하는 의미 기반 색 묶음이다.
///
/// 색을 `AppColors` 상수로 직접 참조하면 다크 모드에서 값을 바꿀 방법이 없다. 테마 확장에
/// 실어 두면 같은 위젯 코드가 밝기에 따라 다른 값을 받는다. 필드 이름은 색상값이 아니라
/// 쓰임(카드 배경, 본문 글자 등)을 가리키므로 밝기가 바뀌어도 호출부는 그대로 둔다.
@immutable
class AppPalette extends ThemeExtension<AppPalette> {
  const AppPalette({
    required this.primaryDark,
    required this.primaryMedium,
    required this.primary,
    required this.primaryLight,
    required this.blue200,
    required this.softBlue,
    required this.blue50,
    required this.scaffold,
    required this.surface,
    required this.card,
    required this.textStrong,
    required this.textPrimary,
    required this.textSecondary,
    required this.textMuted,
    required this.border,
    required this.line,
    required this.lineSubtle,
    required this.borderStrong,
    required this.errorBorder,
    required this.neutralFill,
    required this.recordAccent,

    required this.success,
    required this.successSoft,
    required this.warning,
    required this.warningSoft,
    required this.error,
    required this.errorSoft,
    required this.scoreMedium,
    required this.scoreMediumSoft,
    required this.scoreMediumBorder,
    required this.disabled,
    required this.shadow,
    required this.onPrimary,
  });

  final Color primaryDark;
  final Color primaryMedium;
  final Color primary;
  final Color primaryLight;
  final Color blue200;
  final Color softBlue;
  final Color blue50;
  final Color scaffold;
  final Color surface;
  final Color card;
  final Color textStrong;
  final Color textPrimary;
  final Color textSecondary;
  final Color textMuted;
  final Color border;

  /// 섹션을 가르는 선. 리스트 행 구분선(lineSubtle)보다 진하다.
  final Color line;

  /// 리스트 행 사이의 얇은 선.
  final Color lineSubtle;

  /// 버튼 테두리. 카드 테두리(border)보다 진해 눌리는 것임을 구분한다.
  final Color borderStrong;

  /// 파괴적 동작 버튼의 테두리. 채우지 않고 선으로만 경고한다.
  final Color errorBorder;

  /// 재생 원형 버튼과 비활성 버튼의 채움.
  final Color neutralFill;

  /// 녹음 정지 사각형. 인터랙티브 파랑과 구분되는 유일한 붉은 컨트롤이다.
  final Color recordAccent;
  final Color success;
  final Color successSoft;
  final Color warning;
  final Color warningSoft;
  final Color error;
  final Color errorSoft;

  /// 60점 이상 80점 미만을 나타내는 점수 전용 주황색 조합이다.
  /// 일반 경고와 평가 결과는 의미가 달라 서로의 색을 공유하지 않는다.
  final Color scoreMedium;
  final Color scoreMediumSoft;
  final Color scoreMediumBorder;
  final Color disabled;
  final Color shadow;

  /// 강조 배경 위에 올리는 글자색이다. 밝은 테마에서는 흰색이지만 어두운 테마에서는
  /// 강조색 자체가 밝아지므로 대비를 유지하려면 어두운 글자를 써야 한다.
  final Color onPrimary;

  /// `docs/design-repair`에서 확정한 파란 카드형 밝은 테마 값이다.
  static const light = AppPalette(
    primaryDark: AppColors.primaryDark,
    primaryMedium: AppColors.primaryMedium,
    primary: AppColors.primary,
    primaryLight: AppColors.primaryLight,
    blue200: AppColors.blue200,
    softBlue: AppColors.softBlue,
    blue50: AppColors.blue50,
    scaffold: AppColors.scaffold,
    surface: AppColors.surface,
    card: AppColors.card,
    textStrong: AppColors.textStrong,
    textPrimary: AppColors.textPrimary,
    textSecondary: AppColors.textSecondary,
    textMuted: AppColors.textMuted,
    border: AppColors.border,
    line: AppColors.line,
    lineSubtle: AppColors.lineSubtle,
    borderStrong: AppColors.borderStrong,
    errorBorder: AppColors.errorBorder,
    neutralFill: AppColors.neutralFill,
    recordAccent: AppColors.recordAccent,
    success: AppColors.success,
    successSoft: AppColors.successSoft,
    warning: AppColors.warning,
    warningSoft: AppColors.warningSoft,
    error: AppColors.error,
    errorSoft: AppColors.errorSoft,
    scoreMedium: AppColors.scoreMedium,
    scoreMediumSoft: AppColors.scoreMediumSoft,
    scoreMediumBorder: AppColors.scoreMediumBorder,
    disabled: AppColors.disabled,
    shadow: AppColors.shadow,
    onPrimary: Colors.white,
  );

  /// 어두운 테마 값이다.
  ///
  /// 밝은 테마 색을 그대로 반전하지 않았다. 어두운 배경에서는 같은 채도의 파랑이 탁해 보여
  /// 강조색을 밝은 쪽으로 올리고, 본문 글자는 순백 대신 살짝 낮춘 회백색을 써서
  /// 어두운 화면에서 생기는 눈부심과 글자 번짐을 줄였다. 상태색도 어두운 배경 위에서
  /// 본문 기준 대비를 넘도록 밝기를 올렸다.
  static const dark = AppPalette(
    primaryDark: Color(0xFFBBD6F0),
    primaryMedium: Color(0xFF7FB2E0),
    primary: Color(0xFF6BA6DC),
    primaryLight: Color(0xFF4F91D1),
    blue200: Color(0xFF2C4356),
    softBlue: Color(0xFF1B2C3B),
    blue50: Color(0xFF16242F),
    scaffold: Color(0xFF0F1720),
    surface: Color(0xFF141E28),
    card: Color(0xFF18242F),
    textStrong: Color(0xFFF2F6F9),
    textPrimary: Color(0xFFE4EBF1),
    textSecondary: Color(0xFFA8B8C6),
    textMuted: Color(0xFF8DA0B0),
    border: Color(0xFF2B3B49),
    line: Color(0xFF243543),
    lineSubtle: Color(0xFF1F2E3A),
    borderStrong: Color(0xFF35485A),
    errorBorder: Color(0xFF5E3236),
    neutralFill: Color(0xFF20303D),
    recordAccent: Color(0xFFE2726A),
    success: Color(0xFF6FCFAA),
    successSoft: Color(0xFF16302A),
    warning: Color(0xFFE0B366),
    warningSoft: Color(0xFF322715),
    error: Color(0xFFF08C8C),
    errorSoft: Color(0xFF33191C),
    scoreMedium: Color(0xFFF0B35F),
    scoreMediumSoft: Color(0xFF322715),
    scoreMediumBorder: Color(0xFF684B24),
    // 어두운 채움(neutralFill) 위에서 본문 기준을 넘도록 밝힌 값이다.
    // 비활성 버튼도 '왜 못 누르는지'를 알려주므로 읽혀야 한다.
    disabled: Color(0xFF93A2AE),
    shadow: Color(0x66000000),
    onPrimary: Color(0xFF0F1720),
  );

  @override
  AppPalette copyWith({
    Color? primaryDark,
    Color? primaryMedium,
    Color? primary,
    Color? primaryLight,
    Color? blue200,
    Color? softBlue,
    Color? blue50,
    Color? scaffold,
    Color? surface,
    Color? card,
    Color? textStrong,
    Color? textPrimary,
    Color? textSecondary,
    Color? textMuted,
    Color? border,
    Color? line,
    Color? lineSubtle,
    Color? borderStrong,
    Color? errorBorder,
    Color? neutralFill,
    Color? recordAccent,

    Color? success,
    Color? successSoft,
    Color? warning,
    Color? warningSoft,
    Color? error,
    Color? errorSoft,
    Color? scoreMedium,
    Color? scoreMediumSoft,
    Color? scoreMediumBorder,
    Color? disabled,
    Color? shadow,
    Color? onPrimary,
  }) {
    return AppPalette(
      primaryDark: primaryDark ?? this.primaryDark,
      primaryMedium: primaryMedium ?? this.primaryMedium,
      primary: primary ?? this.primary,
      primaryLight: primaryLight ?? this.primaryLight,
      blue200: blue200 ?? this.blue200,
      softBlue: softBlue ?? this.softBlue,
      blue50: blue50 ?? this.blue50,
      scaffold: scaffold ?? this.scaffold,
      surface: surface ?? this.surface,
      card: card ?? this.card,
      textStrong: textStrong ?? this.textStrong,
      textPrimary: textPrimary ?? this.textPrimary,
      textSecondary: textSecondary ?? this.textSecondary,
      textMuted: textMuted ?? this.textMuted,
      border: border ?? this.border,
      line: line ?? this.line,
      lineSubtle: lineSubtle ?? this.lineSubtle,
      borderStrong: borderStrong ?? this.borderStrong,
      errorBorder: errorBorder ?? this.errorBorder,
      neutralFill: neutralFill ?? this.neutralFill,
      recordAccent: recordAccent ?? this.recordAccent,

      success: success ?? this.success,
      successSoft: successSoft ?? this.successSoft,
      warning: warning ?? this.warning,
      warningSoft: warningSoft ?? this.warningSoft,
      error: error ?? this.error,
      errorSoft: errorSoft ?? this.errorSoft,
      scoreMedium: scoreMedium ?? this.scoreMedium,
      scoreMediumSoft: scoreMediumSoft ?? this.scoreMediumSoft,
      scoreMediumBorder: scoreMediumBorder ?? this.scoreMediumBorder,
      disabled: disabled ?? this.disabled,
      shadow: shadow ?? this.shadow,
      onPrimary: onPrimary ?? this.onPrimary,
    );
  }

  @override
  AppPalette lerp(ThemeExtension<AppPalette>? other, double t) {
    if (other is! AppPalette) {
      return this;
    }
    Color mix(Color a, Color b) => Color.lerp(a, b, t)!;
    return AppPalette(
      primaryDark: mix(primaryDark, other.primaryDark),
      primaryMedium: mix(primaryMedium, other.primaryMedium),
      primary: mix(primary, other.primary),
      primaryLight: mix(primaryLight, other.primaryLight),
      blue200: mix(blue200, other.blue200),
      softBlue: mix(softBlue, other.softBlue),
      blue50: mix(blue50, other.blue50),
      scaffold: mix(scaffold, other.scaffold),
      surface: mix(surface, other.surface),
      card: mix(card, other.card),
      textStrong: mix(textStrong, other.textStrong),
      textPrimary: mix(textPrimary, other.textPrimary),
      textSecondary: mix(textSecondary, other.textSecondary),
      textMuted: mix(textMuted, other.textMuted),
      border: mix(border, other.border),
      line: mix(line, other.line),
      lineSubtle: mix(lineSubtle, other.lineSubtle),
      borderStrong: mix(borderStrong, other.borderStrong),
      errorBorder: mix(errorBorder, other.errorBorder),
      neutralFill: mix(neutralFill, other.neutralFill),
      recordAccent: mix(recordAccent, other.recordAccent),

      success: mix(success, other.success),
      successSoft: mix(successSoft, other.successSoft),
      warning: mix(warning, other.warning),
      warningSoft: mix(warningSoft, other.warningSoft),
      error: mix(error, other.error),
      errorSoft: mix(errorSoft, other.errorSoft),
      scoreMedium: mix(scoreMedium, other.scoreMedium),
      scoreMediumSoft: mix(scoreMediumSoft, other.scoreMediumSoft),
      scoreMediumBorder: mix(scoreMediumBorder, other.scoreMediumBorder),
      disabled: mix(disabled, other.disabled),
      shadow: mix(shadow, other.shadow),
      onPrimary: mix(onPrimary, other.onPrimary),
    );
  }
}

/// 화면에서 `context.palette.card`처럼 짧게 현재 밝기의 색을 읽게 한다.
extension AppPaletteContext on BuildContext {
  AppPalette get palette =>
      Theme.of(this).extension<AppPalette>() ?? AppPalette.light;
}
