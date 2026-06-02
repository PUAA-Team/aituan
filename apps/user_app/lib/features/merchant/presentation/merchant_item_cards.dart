import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../core/widgets/mock_thumb.dart';
import '../../../core/widgets/price_text.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';

IconData businessIcon(BusinessType type) => switch (type) {
  BusinessType.takeaway => Icons.delivery_dining,
  BusinessType.hotel => Icons.hotel,
  BusinessType.movie => Icons.movie,
  BusinessType.massage => Icons.spa,
  BusinessType.beauty => Icons.face_retouching_natural,
  BusinessType.ticket => Icons.confirmation_number,
  BusinessType.entertainment => Icons.sports_esports,
  BusinessType.groupBuy => Icons.local_activity,
};

String merchantDistanceText(MerchantModel merchant) =>
    merchant.estimatedTimeText.isEmpty
    ? merchant.distance
    : '${merchant.distance} · ${merchant.estimatedTimeText}';

String _distanceText(MerchantModel merchant) => merchantDistanceText(merchant);

class ItemMiniCard extends StatelessWidget {
  const ItemMiniCard({
    super.key,
    required this.item,
    required this.onTap,
    this.width = 136,
  });

  final ItemModel item;
  final VoidCallback onTap;
  final double width;

  @override
  Widget build(BuildContext context) => SizedBox(
    width: width,
    child: AppCard(
      margin: EdgeInsets.zero,
      padding: const EdgeInsets.all(8),
      onTap: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          MockThumb(
            width: double.infinity,
            height: 76,
            icon: businessIcon(item.type),
            label: item.category,
            imageUrl: item.coverUrl,
          ),
          const SizedBox(height: 8),
          Text(
            item.title,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.labelMedium,
          ),
          const SizedBox(height: 5),
          Row(
            children: [
              Expanded(child: PriceText(item.price, size: 16)),
              BrandTag(item.type.label, emphasis: item.type.isTakeaway),
            ],
          ),
        ],
      ),
    ),
  );
}

class MerchantItemCarousel extends StatelessWidget {
  const MerchantItemCarousel({
    super.key,
    required this.items,
    required this.onTap,
  });

  final List<ItemModel> items;
  final ValueChanged<ItemModel> onTap;

  @override
  Widget build(BuildContext context) => SizedBox(
    height: 166,
    child: ListView.separated(
      scrollDirection: Axis.horizontal,
      itemCount: items.length,
      separatorBuilder: (_, _) => const SizedBox(width: 8),
      itemBuilder: (_, index) =>
          ItemMiniCard(item: items[index], onTap: () => onTap(items[index])),
    ),
  );
}

class MerchantAggregateCard extends StatelessWidget {
  const MerchantAggregateCard({
    super.key,
    required this.merchant,
    required this.onMerchantTap,
    required this.onItemTap,
  });

  final MerchantModel merchant;
  final VoidCallback onMerchantTap;
  final ValueChanged<ItemModel> onItemTap;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: onMerchantTap,
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            MockThumb(
              size: 72,
              icon: businessIcon(merchant.type),
              label: merchant.type.label,
              imageUrl: merchant.coverUrl,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    merchant.name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '${merchant.rating}分 · ${_distanceText(merchant)} · ${merchant.summary}',
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, color: AppColors.textLight),
          ],
        ),
        const SizedBox(height: 10),
        MerchantItemCarousel(items: merchant.items, onTap: onItemTap),
      ],
    ),
  );
}
