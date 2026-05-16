import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/route_constants.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/item_model.dart';
import '../../home/data/mock_data.dart';
import 'home_widgets.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final _controller = ScrollController();
  int _visible = 6;

  @override
  void initState() {
    super.initState();
    _controller.addListener(_loadMoreIfNeeded);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final items = allItems.take(_visible).toList();
    return SafeArea(
      child: ListView(
        controller: _controller,
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
        children: [
          const HomeHeroHeader(),
          const HomeModuleGrid(),
          HomeRecommendSection(
            items: items,
            onTap: (item) => _openItem(context, item),
          ),
          if (_visible >= allItems.length)
            Padding(
              padding: const EdgeInsets.only(top: 6, bottom: 10),
              child: Text(
                '已展示更多附近好店',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ),
        ],
      ),
    );
  }

  void _loadMoreIfNeeded() {
    if (!_controller.hasClients || _visible >= allItems.length) return;
    if (_controller.position.extentAfter < 260) {
      setState(() => _visible = (_visible + 4).clamp(0, allItems.length));
    }
  }

  void _openItem(BuildContext context, ItemModel item) {
    if (item.type.isTakeaway) {
      Navigator.pushNamed(
        context,
        Routes.merchantDetail,
        arguments: MerchantArgs(
          type: item.type,
          merchant: merchantById(item.storeId),
        ),
      );
      return;
    }
    Navigator.pushNamed(context, Routes.itemDetail, arguments: ItemArgs(item));
  }
}
