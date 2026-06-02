import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../constants/app_colors.dart';
import '../constants/app_tokens.dart';
import 'cached_app_image.dart';

class MockThumb extends StatelessWidget {
  const MockThumb({
    super.key,
    this.size = AppTokens.thumbList,
    this.width,
    this.height,
    this.icon = Icons.storefront,
    this.radius,
    this.label,
    this.imageUrl,
    this.accentColor = AppColors.brand,
  });

  final double size;
  final double? width;
  final double? height;
  final IconData icon;
  final double? radius;
  final String? label;
  final String? imageUrl;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    final boxWidth = width ?? size;
    final boxHeight = height ?? size;
    final placeholder = _PlaceholderThumb(
      width: boxWidth,
      height: boxHeight,
      icon: icon,
      label: label,
      accentColor: accentColor,
    );
    return ClipRRect(
      borderRadius: BorderRadius.circular(radius ?? AppTokens.radiusSmall),
      child: CachedAppImage(
        imageUrl: imageUrl,
        width: boxWidth,
        height: boxHeight,
        placeholder: placeholder,
      ),
    );
  }
}

class _PlaceholderThumb extends StatelessWidget {
  const _PlaceholderThumb({
    required this.width,
    required this.height,
    required this.icon,
    required this.label,
    required this.accentColor,
  });

  final double width;
  final double height;
  final IconData icon;
  final String? label;
  final Color accentColor;

  @override
  Widget build(BuildContext context) {
    final base = math.min(width, height);
    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: AppColors.soft,
        border: Border.all(color: AppColors.line),
      ),
      child: Stack(
        children: [
          Positioned(
            right: -base * .16,
            top: -base * .12,
            child: _SoftDot(
              size: base * .48,
              color: accentColor.withValues(alpha: .09),
            ),
          ),
          Positioned(
            left: -base * .14,
            bottom: -base * .18,
            child: _SoftDot(
              size: base * .62,
              color: accentColor.withValues(alpha: .07),
            ),
          ),
          Center(
            child: Icon(icon, size: base * .32, color: accentColor),
          ),
          if (label != null)
            Positioned(
              left: 6,
              right: 6,
              bottom: 6,
              child: Text(
                label!,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontSize: 10,
                  fontWeight: FontWeight.w700,
                  color: AppColors.textSub,
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _SoftDot extends StatelessWidget {
  const _SoftDot({required this.size, required this.color});

  final double size;
  final Color color;

  @override
  Widget build(BuildContext context) => Container(
    width: size,
    height: size,
    decoration: BoxDecoration(color: color, shape: BoxShape.circle),
  );
}
