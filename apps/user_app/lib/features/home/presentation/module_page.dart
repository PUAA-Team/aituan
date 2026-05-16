import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_search_box.dart';
import '../../../core/widgets/mock_thumb.dart';
import '../../../core/widgets/price_text.dart';
import '../../../core/widgets/section_header.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';
import '../../../shared/models/module_entry.dart';
import '../../merchant/presentation/merchant_category_widgets.dart';
import '../data/mock_data.dart';

class ModulePage extends StatelessWidget {
  const ModulePage({super.key, required this.module});

  final ModuleEntry module;

  @override
  Widget build(BuildContext context) {
    final moduleItems = itemsByType(module.type);
    final moduleMerchants = merchantsByType(module.type);
    return Scaffold(
      appBar: AppBar(title: Text(module.title)),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          AppSearchBox(
            hint: '搜索${module.title}商家和服务',
            onTap: () => Navigator.pushNamed(context, Routes.search),
          ),
          const SizedBox(height: 6),
          const SectionHeader(title: '热门推荐', action: '爱团优选'),
          _HotRow(items: moduleItems.take(3).toList()),
          SectionHeader(title: '${module.title}精选', action: '附近好店'),
          for (final merchant in moduleMerchants)
            MerchantAggregateCard(
              merchant: merchant,
              onMerchantTap: () => _openMerchant(context, merchant),
              onItemTap: (item) => _openItem(context, item),
            ),
        ],
      ),
    );
  }

  void _openMerchant(BuildContext context, MerchantModel merchant) =>
      Navigator.pushNamed(
        context,
        Routes.merchantDetail,
        arguments: MerchantArgs(type: merchant.type, merchant: merchant),
      );

  void _openItem(BuildContext context, ItemModel item) {
    if (item.type.isTakeaway) {
      _openMerchant(context, merchantById(item.storeId));
      return;
    }
    Navigator.pushNamed(context, Routes.itemDetail, arguments: ItemArgs(item));
  }
}

class _HotRow extends StatelessWidget {
  const _HotRow({required this.items});

  final List<ItemModel> items;

  @override
  Widget build(BuildContext context) => Row(
    children: [
      for (var i = 0; i < items.length; i++)
        Expanded(
          child: Padding(
            padding: EdgeInsets.only(right: i == items.length - 1 ? 0 : 8),
            child: _HotCard(item: items[i]),
          ),
        ),
    ],
  );
}

class _HotCard extends StatelessWidget {
  const _HotCard({required this.item});

  final ItemModel item;

  @override
  Widget build(BuildContext context) => AppCard(
    margin: EdgeInsets.zero,
    padding: const EdgeInsets.all(8),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        MockThumb(
          width: double.infinity,
          height: 70,
          icon: businessIcon(item.type),
          label: item.category,
        ),
        const SizedBox(height: 8),
        Text(
          item.title,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: Theme.of(context).textTheme.labelMedium,
        ),
        const SizedBox(height: 2),
        PriceText(item.price, size: 16),
      ],
    ),
  );
}
