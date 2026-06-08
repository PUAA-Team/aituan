import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_search_box.dart';
import '../../../core/widgets/mock_thumb.dart';
import '../../../core/widgets/price_text.dart';
import '../../../core/widgets/section_header.dart';
import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';
import '../../../shared/models/module_entry.dart';
import '../../location/presentation/location_picker_button.dart';
import '../../merchant/presentation/merchant_item_cards.dart';
import '../data/backend_app_repository.dart';

class ModulePage extends StatefulWidget {
  const ModulePage({super.key, required this.module});

  final ModuleEntry module;

  @override
  State<ModulePage> createState() => _ModulePageState();
}

class _ModulePageState extends State<ModulePage> {
  bool _loading = true;
  Object? _error;
  ModuleData? _data;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return Scaffold(
        appBar: AppBar(title: Text(widget.module.title)),
        body: const Center(child: CircularProgressIndicator()),
      );
    }
    if (_error != null) {
      return Scaffold(
        appBar: AppBar(title: Text(widget.module.title)),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: AppCard(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text('模块数据加载失败'),
                  const SizedBox(height: 8),
                  Text(
                    _error.toString(),
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  const SizedBox(height: 12),
                  FilledButton(onPressed: _load, child: const Text('重试')),
                ],
              ),
            ),
          ),
        ),
      );
    }
    final data = _data!;
    final itemsByStore = _itemsByStore(data.featuredItems);
    return Scaffold(
      appBar: AppBar(title: Text(widget.module.title)),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          children: [
            AppSearchBox(
              hint: '搜索${widget.module.title}商家和服务',
              onTap: () => Navigator.pushNamed(context, Routes.search),
            ),
            const SizedBox(height: 8),
            LocationPickerButton(compact: true, onLocationChanged: _load),
            const SizedBox(height: 6),
            const SectionHeader(title: '热门推荐', action: '爱团优选'),
            _HotRow(
              items: data.featuredItems.take(3).toList(),
              onTap: (item) => _openItem(context, item),
            ),
            SectionHeader(title: '${widget.module.title}精选', action: '附近好店'),
            for (final merchant in data.merchants)
              MerchantAggregateCard(
                merchant: _decorateMerchant(
                  merchant,
                  itemsByStore[merchant.id],
                ),
                onMerchantTap: () => _openMerchant(
                  context,
                  _decorateMerchant(merchant, itemsByStore[merchant.id]),
                ),
                onItemTap: (item) => _openItem(context, item),
              ),
          ],
        ),
      ),
    );
  }

  Future<void> _load() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final data = await backendRepository.fetchModule(widget.module.code);
      if (!mounted) return;
      setState(() {
        _data = data;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error;
        _loading = false;
      });
    }
  }

  Map<String, List<ItemModel>> _itemsByStore(List<ItemModel> items) {
    final map = <String, List<ItemModel>>{};
    for (final item in items) {
      map.putIfAbsent(item.storeId, () => []).add(item);
    }
    return map;
  }

  MerchantModel _decorateMerchant(
    MerchantModel merchant,
    List<ItemModel>? items,
  ) {
    final decorated = items == null || items.isEmpty ? merchant.items : items;
    return MerchantModel(
      id: merchant.id,
      name: merchant.name,
      type: merchant.type,
      distance: merchant.distance,
      rating: merchant.rating,
      summary: merchant.summary,
      address: merchant.address,
      tags: merchant.tags,
      items: decorated,
      coverUrl: merchant.coverUrl,
      recommendReason: merchant.recommendReason,
      estimatedTimeText: merchant.estimatedTimeText,
      longitude: merchant.longitude,
      latitude: merchant.latitude,
      status: merchant.status,
      businessHours: merchant.businessHours,
      monthlySales: merchant.monthlySales,
      avgPrice: merchant.avgPrice,
      deliveryRule: merchant.deliveryRule,
    );
  }

  void _openMerchant(BuildContext context, MerchantModel merchant) =>
      Navigator.pushNamed(
        context,
        Routes.merchantDetail,
        arguments: MerchantArgs(type: merchant.type, merchant: merchant),
      );

  void _openItem(BuildContext context, ItemModel item) {
    Navigator.pushNamed(context, Routes.itemDetail, arguments: ItemArgs(item));
  }
}

class _HotRow extends StatelessWidget {
  const _HotRow({required this.items, required this.onTap});

  final List<ItemModel> items;
  final ValueChanged<ItemModel> onTap;

  @override
  Widget build(BuildContext context) => Row(
    children: [
      for (var i = 0; i < items.length; i++)
        Expanded(
          child: Padding(
            padding: EdgeInsets.only(right: i == items.length - 1 ? 0 : 8),
            child: _HotCard(item: items[i], onTap: () => onTap(items[i])),
          ),
        ),
    ],
  );
}

class _HotCard extends StatelessWidget {
  const _HotCard({required this.item, required this.onTap});

  final ItemModel item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: onTap,
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
          imageUrl: item.coverUrl,
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
