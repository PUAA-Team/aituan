import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../core/widgets/mock_thumb.dart';
import '../../../core/widgets/price_text.dart';
import '../../../core/widgets/section_header.dart';
import '../../../shared/enums/business_type.dart';
import '../../home/data/mock_data.dart';

class FavoritePage extends StatelessWidget {
  const FavoritePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('我的收藏')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          const SectionHeader(title: '收藏商家'),
          const SizedBox(height: 8),
          for (final merchant in merchants.take(2))
            AppCard(
              onTap: () => Navigator.pushNamed(
                context,
                Routes.merchantDetail,
                arguments: MerchantArgs(
                  type: merchant.type,
                  merchant: merchant,
                ),
              ),
              child: Row(
                children: [
                  MockThumb(
                    size: 72,
                    icon: merchant.type.isTakeaway
                        ? Icons.delivery_dining
                        : Icons.storefront,
                    label: merchant.type.label,
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
                          merchant.summary,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8),
                  BrandTag(
                    merchant.type.label,
                    emphasis: merchant.type.isTakeaway,
                  ),
                ],
              ),
            ),
          const SizedBox(height: 8),
          const SectionHeader(title: '收藏商品/服务'),
          const SizedBox(height: 8),
          for (final item in serviceItems.take(2))
            AppCard(
              onTap: () => Navigator.pushNamed(
                context,
                Routes.itemDetail,
                arguments: ItemArgs(item),
              ),
              child: Row(
                children: [
                  MockThumb(
                    size: 72,
                    icon: Icons.local_activity,
                    label: item.type.label,
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
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                        const SizedBox(height: 5),
                        BrandTag(item.type.label),
                      ],
                    ),
                  ),
                  PriceText(item.price, size: 18),
                ],
              ),
            ),
        ],
      ),
    );
  }
}
