import 'package:flutter/material.dart';

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
