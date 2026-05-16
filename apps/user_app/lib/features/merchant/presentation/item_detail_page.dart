import 'package:flutter/material.dart';

import '../../../app/app_state.dart';
import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_bottom_action_bar.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../core/widgets/mock_thumb.dart';
import '../../../core/widgets/price_text.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/item_model.dart';
import '../../home/data/mock_data.dart';

class ItemDetailPage extends StatelessWidget {
  const ItemDetailPage({super.key, required this.item});

  final ItemModel item;

  @override
  Widget build(BuildContext context) {
    final merchant = merchantById(item.storeId);
    return Scaffold(
      appBar: AppBar(title: const Text('商品/服务详情')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          MockThumb(
            width: double.infinity,
            height: 190,
            icon: Icons.local_activity,
            label: item.type.label,
          ),
          const SizedBox(height: 10),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                PriceText(item.price, size: 26),
                const SizedBox(height: 8),
                Text(
                  item.title,
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
                const SizedBox(height: 8),
                Text(
                  item.subtitle,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: [
                    for (final tag in item.tags) BrandTag(tag, emphasis: true),
                  ],
                ),
              ],
            ),
          ),
          const AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '购买须知',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
                ),
                SizedBox(height: 8),
                Text(
                  '支付成功后生成券码，未使用可退；高峰时段建议提前电话确认。',
                  style: TextStyle(fontSize: 13, color: AppColors.textSub),
                ),
              ],
            ),
          ),
          AppCard(
            onTap: () => Navigator.pushNamed(
              context,
              Routes.merchantDetail,
              arguments: MerchantArgs(type: merchant.type, merchant: merchant),
            ),
            child: Row(
              children: [
                const Icon(Icons.storefront, color: AppColors.brand),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    merchant.name,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                const Icon(Icons.chevron_right, color: AppColors.textSub),
              ],
            ),
          ),
          const SizedBox(height: 80),
        ],
      ),
      bottomNavigationBar: AppBottomActionBar(
        primaryText: '立即购买',
        onPrimary: () => _buy(context),
        secondaryText: '收藏',
        onSecondary: () => AppScope.of(context).requireLogin(context),
      ),
    );
  }

  void _buy(BuildContext context) {
    if (!AppScope.of(context).requireLogin(context)) return;
    Navigator.pushNamed(
      context,
      Routes.checkout,
      arguments: CheckoutArgs(
        kind: OrderKind.service,
        title: item.title,
        amount: item.price,
      ),
    );
  }
}
