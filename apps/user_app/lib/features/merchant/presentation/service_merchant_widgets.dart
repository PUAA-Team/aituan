import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../core/widgets/mock_thumb.dart';
import '../../../core/widgets/price_text.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';
import 'merchant_category_widgets.dart';

class ServiceMerchantHero extends StatelessWidget {
  const ServiceMerchantHero({super.key, required this.merchant});

  final MerchantModel merchant;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            MockThumb(
              size: 84,
              icon: businessIcon(merchant.type),
              label: merchant.type.label,
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
                    '${merchant.rating}分 · ${merchantDistanceText(merchant)}',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 6,
                    runSpacing: 6,
                    children: [
                      for (final tag in merchant.tags)
                        BrandTag(tag, green: tag.contains('预约')),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Text(merchant.summary, style: Theme.of(context).textTheme.bodyMedium),
        const SizedBox(height: 8),
        Row(
          children: [
            const Icon(
              Icons.place_outlined,
              size: 18,
              color: AppColors.textSub,
            ),
            const SizedBox(width: 4),
            Expanded(
              child: Text(
                merchant.address,
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ),
          ],
        ),
      ],
    ),
  );
}

class ServiceCategoryPanel extends StatelessWidget {
  const ServiceCategoryPanel({
    super.key,
    required this.groups,
    required this.activeCategory,
    required this.onSelected,
  });

  final Map<String, List<ItemModel>> groups;
  final String activeCategory;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) => CategoryGroupedList(
    groups: groups,
    activeCategory: activeCategory,
    emptyText: '该商家暂未上架服务',
    headerAction: '到店核销',
    onSelected: onSelected,
    itemBuilder: (_, item) => _ServiceItem(item: item),
  );
}

class ServiceReviewPanel extends StatelessWidget {
  const ServiceReviewPanel({super.key});

  @override
  Widget build(BuildContext context) => const Column(
    children: [
      AppCard(
        child: Text(
          '4.7 分 · 核销顺利 · 环境稳定 · 服务态度好',
          style: TextStyle(fontWeight: FontWeight.w700),
        ),
      ),
      AppCard(
        child: Text(
          '预约后到店很顺畅，服务项目说明清楚，券码核销也很快。',
          style: TextStyle(color: AppColors.textSub),
        ),
      ),
      AppCard(
        child: Text(
          '门店位置好找，适合周末和朋友一起到店体验。',
          style: TextStyle(color: AppColors.textSub),
        ),
      ),
    ],
  );
}

class ServiceMerchantInfoPanel extends StatelessWidget {
  const ServiceMerchantInfoPanel({super.key, required this.merchant});

  final MerchantModel merchant;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _ServiceInfoLine(icon: Icons.place_outlined, text: merchant.address),
        const _ServiceInfoLine(icon: Icons.schedule, text: '营业时间 10:00-22:00'),
        const _ServiceInfoLine(
          icon: Icons.confirmation_number_outlined,
          text: '购买后到店出示券码或二维码核销，部分项目建议提前预约',
        ),
        const SizedBox(height: 8),
        Text(merchant.summary, style: Theme.of(context).textTheme.bodyMedium),
      ],
    ),
  );
}

class _ServiceInfoLine extends StatelessWidget {
  const _ServiceInfoLine({required this.icon, required this.text});

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

class _ServiceItem extends StatelessWidget {
  const _ServiceItem({required this.item});

  final ItemModel item;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: () => Navigator.pushNamed(
      context,
      Routes.itemDetail,
      arguments: ItemArgs(item),
    ),
    child: Row(
      children: [
        MockThumb(
          size: 78,
          icon: businessIcon(item.type),
          label: item.category,
          imageUrl: item.coverUrl,
        ),
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
              const SizedBox(height: 5),
              Text(item.subtitle, style: Theme.of(context).textTheme.bodySmall),
              const SizedBox(height: 8),
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: [for (final tag in item.tags) BrandTag(tag)],
              ),
            ],
          ),
        ),
        const SizedBox(width: 8),
        PriceText(item.price, size: 18),
      ],
    ),
  );
}
