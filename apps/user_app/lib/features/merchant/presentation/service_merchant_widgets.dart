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
                    '${merchant.rating}分 · ${merchant.distance}',
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
