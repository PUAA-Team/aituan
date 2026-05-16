import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../core/widgets/mock_thumb.dart';
import '../../../core/widgets/price_text.dart';
import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';

class TakeawayMerchantHeader extends StatelessWidget {
  const TakeawayMerchantHeader({super.key, required this.merchant});

  final MerchantModel merchant;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Row(
      children: [
        const MockThumb(size: 76, icon: Icons.delivery_dining, label: '外卖'),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                merchant.name,
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 4),
              Text(
                '${merchant.rating}分 · ${merchant.distance} · 预计35分钟送达',
                style: Theme.of(context).textTheme.bodySmall,
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: [
                  for (final tag in merchant.tags)
                    BrandTag(tag, emphasis: true),
                ],
              ),
            ],
          ),
        ),
      ],
    ),
  );
}

class TakeawayFoodRow extends StatelessWidget {
  const TakeawayFoodRow({
    super.key,
    required this.item,
    required this.count,
    required this.onAdd,
    required this.onRemove,
  });

  final ItemModel item;
  final int count;
  final VoidCallback onAdd;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Row(
      children: [
        const MockThumb(size: 76, icon: Icons.lunch_dining, label: '热卖'),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                item.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 4),
              Text(item.subtitle, style: Theme.of(context).textTheme.bodySmall),
              const SizedBox(height: 8),
              PriceText(item.price, size: 18),
            ],
          ),
        ),
        _counter(count: count, onAdd: onAdd, onRemove: onRemove),
      ],
    ),
  );
}

class TakeawayCartBar extends StatelessWidget {
  const TakeawayCartBar({
    super.key,
    required this.total,
    required this.count,
    required this.onOpen,
    required this.onSubmit,
  });

  final double total;
  final int count;
  final VoidCallback onOpen;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    final payable = total > 0 ? total + 4 : 0.0;
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
        decoration: const BoxDecoration(
          color: Colors.white,
          border: Border(top: BorderSide(color: AppColors.line)),
        ),
        child: Row(
          children: [
            InkWell(
              onTap: onOpen,
              borderRadius: BorderRadius.circular(8),
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Row(
                  children: [
                    const Icon(Icons.shopping_cart, color: AppColors.brand),
                    const SizedBox(width: 6),
                    Text(
                      '$count 件',
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                  ],
                ),
              ),
            ),
            const Spacer(),
            PriceText(payable, size: 20),
            const SizedBox(width: 12),
            FilledButton(
              onPressed: total > 0 ? onSubmit : null,
              child: const Text('提交'),
            ),
          ],
        ),
      ),
    );
  }
}

Widget _counter({
  required int count,
  required VoidCallback onAdd,
  required VoidCallback onRemove,
}) => Row(
  mainAxisSize: MainAxisSize.min,
  children: [
    if (count > 0) _roundIcon(icon: Icons.remove, onTap: onRemove),
    if (count > 0)
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 6),
        child: Text(
          '$count',
          style: const TextStyle(fontWeight: FontWeight.w700),
        ),
      ),
    _roundIcon(icon: Icons.add, onTap: onAdd, filled: true),
  ],
);

Widget _roundIcon({
  required IconData icon,
  required VoidCallback onTap,
  bool filled = false,
}) => InkWell(
  onTap: onTap,
  borderRadius: BorderRadius.circular(14),
  child: Container(
    width: 28,
    height: 28,
    decoration: BoxDecoration(
      color: filled ? AppColors.brand : AppColors.card,
      shape: BoxShape.circle,
      border: Border.all(color: AppColors.brandLine),
    ),
    child: Icon(icon, size: 18, color: filled ? Colors.white : AppColors.brand),
  ),
);
