import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';
import 'merchant_category_widgets.dart';
import 'takeaway_merchant_widgets.dart';

class TakeawayMerchantTabs extends StatelessWidget {
  const TakeawayMerchantTabs({
    super.key,
    required this.value,
    required this.onChanged,
  });

  final int value;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) => SegmentedButton<int>(
    segments: const [
      ButtonSegment(value: 0, label: Text('下单')),
      ButtonSegment(value: 1, label: Text('评价')),
      ButtonSegment(value: 2, label: Text('商家')),
    ],
    selected: {value},
    onSelectionChanged: (values) => onChanged(values.first),
  );
}

class TakeawayOrderPanel extends StatelessWidget {
  const TakeawayOrderPanel({
    super.key,
    required this.groups,
    required this.activeCategory,
    required this.cart,
    required this.onSelected,
    required this.onAdd,
    required this.onRemove,
  });

  final Map<String, List<ItemModel>> groups;
  final String activeCategory;
  final Map<String, int> cart;
  final ValueChanged<String> onSelected;
  final ValueChanged<ItemModel> onAdd;
  final ValueChanged<ItemModel> onRemove;

  @override
  Widget build(BuildContext context) => CategoryGroupedList(
    groups: groups,
    activeCategory: activeCategory,
    emptyText: '该商家暂未上架商品',
    headerAction: '可直接加购',
    onSelected: onSelected,
    itemBuilder: (_, item) => TakeawayFoodRow(
      item: item,
      count: cart[item.id] ?? 0,
      onAdd: () => onAdd(item),
      onRemove: () => onRemove(item),
    ),
  );
}

class TakeawayReviewPanel extends StatelessWidget {
  const TakeawayReviewPanel({super.key});

  @override
  Widget build(BuildContext context) => const Column(
    children: [
      AppCard(
        child: Text(
          '4.8 分 · 出餐稳定 · 包装完整 · 配送体验好',
          style: TextStyle(fontWeight: FontWeight.w700),
        ),
      ),
      AppCard(
        child: Text(
          '汉堡现做热乎，套餐份量适合单人晚餐，骑手送达也很快。',
          style: TextStyle(color: AppColors.textSub),
        ),
      ),
      AppCard(
        child: Text(
          '多次回购，餐品口味稳定，适合附近工作日点餐。',
          style: TextStyle(color: AppColors.textSub),
        ),
      ),
    ],
  );
}

class TakeawayMerchantInfoPanel extends StatelessWidget {
  const TakeawayMerchantInfoPanel({super.key, required this.merchant});

  final MerchantModel merchant;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _InfoLine(icon: Icons.place_outlined, text: merchant.address),
        _InfoLine(icon: Icons.schedule, text: '营业时间 ${merchant.businessHours}'),
        _InfoLine(
          icon: Icons.delivery_dining,
          text: merchant.deliveryRule.deliveryText.isEmpty
              ? '由商家接单后安排配送，配送费以确认订单页为准'
              : merchant.deliveryRule.deliveryText,
        ),
        _InfoLine(
          icon: Icons.receipt_long_outlined,
          text:
              '起送￥${merchant.deliveryRule.startPrice.toStringAsFixed(0)} · 配送费￥${merchant.deliveryRule.deliveryFee.toStringAsFixed(0)} · ${merchant.deliveryRule.packageFeeText}',
        ),
        _InfoLine(
          icon: Icons.route_outlined,
          text: merchant.deliveryRule.distanceExtraText,
        ),
        const SizedBox(height: 8),
        Text(merchant.summary, style: Theme.of(context).textTheme.bodyMedium),
      ],
    ),
  );
}

class _InfoLine extends StatelessWidget {
  const _InfoLine({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 8),
    child: Row(
      children: [
        Icon(icon, size: 18, color: AppColors.textSub),
        const SizedBox(width: 6),
        Expanded(child: Text(text)),
      ],
    ),
  );
}
