import 'package:flutter/material.dart';

import '../core/constants/app_colors.dart';
import '../core/constants/app_tokens.dart';

ThemeData buildAituanTheme() {
  final scheme = ColorScheme.fromSeed(
    seedColor: AppColors.brand,
    primary: AppColors.brand,
    secondary: AppColors.brandStrong,
    surface: AppColors.card,
  );

  const textTheme = TextTheme(
    headlineMedium: TextStyle(
      fontSize: 22,
      fontWeight: FontWeight.w800,
      color: AppColors.textMain,
      height: 1.25,
    ),
    titleLarge: TextStyle(
      fontSize: 18,
      fontWeight: FontWeight.w800,
      color: AppColors.textMain,
      height: 1.25,
    ),
    titleMedium: TextStyle(
      fontSize: 16,
      fontWeight: FontWeight.w700,
      color: AppColors.textMain,
      height: 1.3,
    ),
    titleSmall: TextStyle(
      fontSize: 15,
      fontWeight: FontWeight.w700,
      color: AppColors.textMain,
      height: 1.3,
    ),
    bodyLarge: TextStyle(
      fontSize: 15,
      fontWeight: FontWeight.w400,
      color: AppColors.textMain,
      height: 1.45,
    ),
    bodyMedium: TextStyle(
      fontSize: 14,
      fontWeight: FontWeight.w400,
      color: AppColors.textMain,
      height: 1.45,
    ),
    bodySmall: TextStyle(
      fontSize: 13,
      fontWeight: FontWeight.w400,
      color: AppColors.textSub,
      height: 1.4,
    ),
    labelLarge: TextStyle(
      fontSize: 14,
      fontWeight: FontWeight.w700,
      height: 1.25,
    ),
    labelMedium: TextStyle(
      fontSize: 13,
      fontWeight: FontWeight.w600,
      height: 1.25,
    ),
    labelSmall: TextStyle(
      fontSize: 12,
      fontWeight: FontWeight.w600,
      height: 1.2,
    ),
  );

  return ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    scaffoldBackgroundColor: AppColors.page,
    fontFamilyFallback: const ['PingFang SC', 'Microsoft YaHei'],
    textTheme: textTheme,
    appBarTheme: const AppBarTheme(
      centerTitle: true,
      elevation: 0,
      backgroundColor: AppColors.card,
      foregroundColor: AppColors.textMain,
      surfaceTintColor: Colors.transparent,
      titleTextStyle: TextStyle(
        fontSize: 17,
        fontWeight: FontWeight.w800,
        color: AppColors.textMain,
      ),
    ),
    cardTheme: const CardThemeData(
      color: AppColors.card,
      elevation: 0,
      surfaceTintColor: Colors.transparent,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(AppTokens.radiusCard)),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: AppColors.soft,
      isDense: true,
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      hintStyle: const TextStyle(color: AppColors.textLight, fontSize: 14),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide.none,
      ),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size(0, 44),
        padding: const EdgeInsets.symmetric(horizontal: 18),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
        textStyle: textTheme.labelLarge,
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        minimumSize: const Size(0, 42),
        padding: const EdgeInsets.symmetric(horizontal: 16),
        side: const BorderSide(color: AppColors.brandLine),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(21)),
        foregroundColor: AppColors.brand,
        textStyle: textTheme.labelLarge,
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: AppColors.brand,
        textStyle: textTheme.labelLarge,
      ),
    ),
    chipTheme: ChipThemeData(
      backgroundColor: AppColors.soft,
      selectedColor: AppColors.brandSoft,
      disabledColor: AppColors.soft,
      side: const BorderSide(color: AppColors.line),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppTokens.radiusTag),
      ),
      labelStyle: textTheme.labelMedium?.copyWith(color: AppColors.textSub),
      secondaryLabelStyle: textTheme.labelMedium?.copyWith(
        color: AppColors.brandStrong,
      ),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
      showCheckmark: false,
    ),
    navigationBarTheme: NavigationBarThemeData(
      height: 64,
      indicatorColor: AppColors.brandSoft,
      backgroundColor: AppColors.card,
      elevation: 0,
      labelTextStyle: WidgetStateProperty.resolveWith((states) {
        final selected = states.contains(WidgetState.selected);
        return TextStyle(
          fontSize: 12,
          fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
          color: selected ? AppColors.brand : AppColors.textSub,
        );
      }),
      iconTheme: WidgetStateProperty.resolveWith((states) {
        final selected = states.contains(WidgetState.selected);
        return IconThemeData(
          size: 24,
          color: selected ? AppColors.brand : AppColors.textSub,
        );
      }),
    ),
    listTileTheme: const ListTileThemeData(
      contentPadding: EdgeInsets.zero,
      titleTextStyle: TextStyle(
        fontSize: 15,
        fontWeight: FontWeight.w700,
        color: AppColors.textMain,
      ),
      subtitleTextStyle: TextStyle(
        fontSize: 13,
        color: AppColors.textSub,
        height: 1.35,
      ),
      minVerticalPadding: 8,
    ),
    segmentedButtonTheme: SegmentedButtonThemeData(
      style: ButtonStyle(
        textStyle: WidgetStateProperty.all(textTheme.labelMedium),
        side: WidgetStateProperty.all(const BorderSide(color: AppColors.line)),
        visualDensity: VisualDensity.compact,
      ),
    ),
  );
}
