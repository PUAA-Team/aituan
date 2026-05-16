import 'package:flutter/material.dart';

import '../constants/app_colors.dart';
import 'price_text.dart';

class AppBottomActionBar extends StatelessWidget {
  const AppBottomActionBar({
    super.key,
    required this.primaryText,
    required this.onPrimary,
    this.secondaryText,
    this.onSecondary,
    this.price,
    this.note,
    this.leading,
  });

  final String primaryText;
  final VoidCallback? onPrimary;
  final String? secondaryText;
  final VoidCallback? onSecondary;
  final double? price;
  final String? note;
  final Widget? leading;

  @override
  Widget build(BuildContext context) => SafeArea(
    top: false,
    child: Container(
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(top: BorderSide(color: AppColors.line)),
      ),
      child: Row(
        children: [
          if (leading != null) ...[
            leading!,
            const SizedBox(width: 12),
          ] else if (price != null || note != null) ...[
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (note != null)
                    Text(
                      note!,
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppColors.textSub,
                      ),
                    ),
                  if (price != null) PriceText(price!, size: 22),
                ],
              ),
            ),
          ] else
            const Spacer(),
          if (secondaryText != null) ...[
            OutlinedButton(onPressed: onSecondary, child: Text(secondaryText!)),
            const SizedBox(width: 10),
          ],
          FilledButton(onPressed: onPrimary, child: Text(primaryText)),
        ],
      ),
    ),
  );
}
