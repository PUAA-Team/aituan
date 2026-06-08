import 'package:flutter/material.dart';

import '../../../../core/widgets/app_card.dart';
import '../../../../core/widgets/brand_tag.dart';
import '../../../../core/widgets/mock_thumb.dart';
import '../../../../core/widgets/price_text.dart';
import '../../../../core/widgets/section_header.dart';
import '../../../../shared/enums/business_type.dart';
import '../../../../shared/models/item_model.dart';

class HomeRecommendSection extends StatelessWidget {
  const HomeRecommendSection({
    super.key,
    required this.items,
    required this.onTap,
  });

  final List<ItemModel> items;
  final ValueChanged<ItemModel> onTap;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      const SectionHeader(title: '猜你喜欢', action: '为你更新'),
      GridView.count(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        crossAxisCount: 2,
        crossAxisSpacing: 10,
        mainAxisSpacing: 10,
        childAspectRatio: .72,
        children: [
          for (final item in items)
            RecommendCard(item: item, onTap: () => onTap(item)),
        ],
      ),
    ],
  );
}

class RecommendCard extends StatelessWidget {
  const RecommendCard({super.key, required this.item, required this.onTap});

  final ItemModel item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => AppCard(
    margin: EdgeInsets.zero,
    padding: EdgeInsets.zero,
    onTap: onTap,
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        MockThumb(
          width: double.infinity,
          height: 112,
          icon: _icon(item.type),
          label: item.type.label,
          imageUrl: item.coverUrl,
        ),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.all(10),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  item.title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.titleSmall,
                ),
                const SizedBox(height: 5),
                Text(
                  item.recommendReason.isEmpty
                      ? item.subtitle
                      : item.recommendReason,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const Spacer(),
                Row(
                  children: [
                    PriceText(item.price, size: 17),
                    const Spacer(),
                    BrandTag(item.type.label, emphasis: item.type.isTakeaway),
                  ],
                ),
              ],
            ),
          ),
        ),
      ],
    ),
  );
}

IconData _icon(BusinessType type) => switch (type) {
  BusinessType.takeaway => Icons.delivery_dining,
  BusinessType.hotel => Icons.hotel,
  BusinessType.movie => Icons.movie,
  BusinessType.massage => Icons.spa,
  BusinessType.beauty => Icons.face_retouching_natural,
  BusinessType.ticket => Icons.confirmation_number,
  BusinessType.entertainment => Icons.sports_esports,
  BusinessType.groupBuy => Icons.local_activity,
};
