import 'package:flutter/material.dart';

import '../constants/app_colors.dart';
import '../constants/app_tokens.dart';

class AppCard extends StatelessWidget {
  const AppCard({
    super.key,
    required this.child,
    this.padding,
    this.margin,
    this.onTap,
    this.radius,
    this.backgroundColor,
    this.borderColor,
  });

  final Widget child;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final VoidCallback? onTap;
  final double? radius;
  final Color? backgroundColor;
  final Color? borderColor;

  @override
  Widget build(BuildContext context) {
    final cardRadius = BorderRadius.circular(radius ?? AppTokens.radiusCard);
    return Padding(
      padding: margin ?? const EdgeInsets.only(bottom: AppTokens.cardGap),
      child: Material(
        color: backgroundColor ?? AppColors.card,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: cardRadius,
          side: BorderSide(color: borderColor ?? AppColors.line),
        ),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          child: Padding(
            padding: padding ?? const EdgeInsets.all(AppTokens.cardPadding),
            child: child,
          ),
        ),
      ),
    );
  }
}
