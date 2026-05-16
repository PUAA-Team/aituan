import 'package:flutter/material.dart';

import '../constants/app_colors.dart';
import '../constants/app_tokens.dart';

class BrandTag extends StatelessWidget {
  const BrandTag(
    this.text, {
    super.key,
    this.emphasis = false,
    this.green = false,
    this.selected = false,
    this.solid = false,
    this.icon,
  });

  final String text;
  final bool emphasis;
  final bool green;
  final bool selected;
  final bool solid;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final isBrand = emphasis || selected || solid;
    final color = green
        ? AppColors.success
        : (isBrand ? AppColors.brandStrong : AppColors.textSub);
    final bg = solid
        ? AppColors.brand
        : green
        ? const Color(0xFFEFFAF5)
        : isBrand
        ? AppColors.brandSoft
        : AppColors.soft;
    final border = green
        ? const Color(0xFFC8EFDD)
        : isBrand
        ? AppColors.brandLine
        : AppColors.line;
    final textColor = solid ? Colors.white : color;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
      decoration: BoxDecoration(
        color: bg,
        border: Border.all(color: solid ? AppColors.brand : border),
        borderRadius: BorderRadius.circular(AppTokens.radiusTag),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 13, color: textColor),
            const SizedBox(width: 4),
          ],
          Text(
            text,
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: textColor,
              height: 1.1,
            ),
          ),
        ],
      ),
    );
  }
}
