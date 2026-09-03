import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../core/widgets/mock_thumb.dart';
import '../../../core/widgets/price_text.dart';
import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';
import 'merchant_item_cards.dart';
import 'takeaway_amount_utils.dart';
import 'takeaway_quantity_stepper.dart';

class TakeawayMerchantHeader extends StatelessWidget {
  const TakeawayMerchantHeader({super.key, required this.merchant});

  final MerchantModel merchant;

  @override
  Widget build(BuildContext context) {
    final rule = merchant.deliveryRule;
    final statusText = merchant.status == 'open' ? '营业中' : '休息中';
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              MockThumb(
                size: 76,
                icon: Icons.delivery_dining,
                label: '外卖',
                imageUrl: merchant.coverUrl,
              ),
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
                      '${merchantRatingText(merchant)} · 月售${merchant.monthlySales} · ${merchantDistanceText(merchant)}',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 6,
                      runSpacing: 6,
                      children: [
                        BrandTag(statusText, emphasis: true),
                        for (final tag in merchant.tags) BrandTag(tag),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              _MerchantMetric(label: '起送', value: _money(rule.startPrice)),
              _MerchantMetric(label: '配送费', value: _money(rule.deliveryFee)),
              _MerchantMetric(
                label: '预计送达',
                value: '${rule.estimatedMinutes}分钟',
              ),
            ],
          ),
          if (rule.deliveryText.isNotEmpty) ...[
            const SizedBox(height: 10),
            Text(
              rule.deliveryText,
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ],
      ),
    );
  }
}

class TakeawayFoodRow extends StatelessWidget {
  const TakeawayFoodRow({
    super.key,
    required this.item,
    required this.count,
    required this.onAdd,
    required this.onRemove,
    this.catalogAvailable = true,
  });

  final ItemModel item;
  final int count;
  final VoidCallback onAdd;
  final VoidCallback onRemove;
  final bool catalogAvailable;

  @override
  Widget build(BuildContext context) {
    final limitReached = count >= item.stock;
    return AppCard(
      padding: const EdgeInsets.all(10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          MockThumb(
            size: 76,
            icon: Icons.lunch_dining,
            label: '热卖',
            imageUrl: item.coverUrl,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  item.subtitle.isEmpty
                      ? '${item.category} · 爱团推荐'
                      : item.subtitle,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const SizedBox(height: 5),
                Wrap(
                  spacing: 6,
                  runSpacing: 4,
                  children: [
                    if (item.soldOut)
                      const _StockTag(text: '已售罄', muted: true)
                    else if (item.stock <= 5)
                      _StockTag(text: '仅剩${item.stock}份'),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Expanded(child: PriceText(item.price, size: 18)),
                    TakeawayQuantityStepper(
                      count: count,
                      canAdd:
                          catalogAvailable && !item.soldOut && !limitReached,
                      onAdd: onAdd,
                      onRemove: onRemove,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class TakeawayCartBar extends StatelessWidget {
  const TakeawayCartBar({
    super.key,
    required this.total,
    required this.count,
    required this.deliveryFee,
    required this.startPrice,
    required this.onOpen,
    required this.onSubmit,
    this.catalogAvailable = true,
  });

  final double total;
  final int count;
  final double deliveryFee;
  final double startPrice;
  final VoidCallback onOpen;
  final VoidCallback onSubmit;
  final bool catalogAvailable;

  @override
  Widget build(BuildContext context) {
    final payable = total > 0 ? total + deliveryFee : 0.0;
    final missing = takeawayStartMissing(total, startPrice);
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
              onPressed: catalogAvailable && total > 0 && missing <= 0
                  ? onSubmit
                  : null,
              child: Text(
                !catalogAvailable
                    ? '服务恢复后结算'
                    : missing > 0
                    ? '差￥${takeawayMoneyText(missing)}起送'
                    : '提交',
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _MerchantMetric extends StatelessWidget {
  const _MerchantMetric({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) => Expanded(
    child: Container(
      margin: const EdgeInsets.only(right: 8),
      padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 10),
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: Theme.of(context).textTheme.bodySmall),
          const SizedBox(height: 2),
          Text(value, style: const TextStyle(fontWeight: FontWeight.w800)),
        ],
      ),
    ),
  );
}

class _StockTag extends StatelessWidget {
  const _StockTag({required this.text, this.muted = false});

  final String text;
  final bool muted;

  @override
  Widget build(BuildContext context) => Container(
    margin: const EdgeInsets.only(left: 8),
    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
    decoration: BoxDecoration(
      color: muted ? AppColors.card : AppColors.brandSoft,
      borderRadius: BorderRadius.circular(6),
      border: Border.all(color: muted ? AppColors.line : AppColors.brandLine),
    ),
    child: Text(
      text,
      style: TextStyle(
        color: muted ? AppColors.textSub : AppColors.brand,
        fontSize: 11,
        fontWeight: FontWeight.w700,
      ),
    ),
  );
}

String _money(double value) =>
    value <= 0 ? '免起送' : '￥${value.toStringAsFixed(0)}';
