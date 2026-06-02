import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';

class TakeawayQuantityStepper extends StatelessWidget {
  const TakeawayQuantityStepper({
    super.key,
    required this.count,
    required this.canAdd,
    required this.onAdd,
    required this.onRemove,
  });

  final int count;
  final bool canAdd;
  final VoidCallback onAdd;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) => Row(
    mainAxisSize: MainAxisSize.min,
    children: [
      if (count > 0) _RoundButton(icon: Icons.remove, onTap: onRemove),
      if (count > 0)
        SizedBox(
          width: 28,
          child: Center(
            child: Text(
              '$count',
              style: const TextStyle(fontWeight: FontWeight.w800),
            ),
          ),
        ),
      _RoundButton(icon: Icons.add, onTap: canAdd ? onAdd : null, filled: true),
    ],
  );
}

class _RoundButton extends StatelessWidget {
  const _RoundButton({
    required this.icon,
    required this.onTap,
    this.filled = false,
  });

  final IconData icon;
  final VoidCallback? onTap;
  final bool filled;

  @override
  Widget build(BuildContext context) => InkWell(
    onTap: onTap,
    borderRadius: BorderRadius.circular(16),
    child: Opacity(
      opacity: onTap == null ? 0.45 : 1,
      child: Container(
        width: 30,
        height: 30,
        decoration: BoxDecoration(
          color: filled ? AppColors.brand : AppColors.card,
          shape: BoxShape.circle,
          border: Border.all(color: AppColors.brandLine),
        ),
        child: Icon(
          icon,
          size: 18,
          color: filled ? Colors.white : AppColors.brand,
        ),
      ),
    ),
  );
}
